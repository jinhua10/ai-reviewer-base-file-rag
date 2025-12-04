#!/usr/bin/env python3
"""
国产向量嵌入模型下载脚本
支持 BGE、Text2Vec 等国产模型

使用方法：
    python download_embedding_model.py --model bge-m3
    python download_embedding_model.py --model bge-base-zh --mirror
"""

import os
import sys
import argparse
from pathlib import Path

def check_dependencies():
    """检查依赖"""
    print("📦 检查依赖...")

    try:
        import sentence_transformers
        print(f"✅ sentence-transformers {sentence_transformers.__version__}")
        return True
    except ImportError:
        print("❌ sentence-transformers 未安装")
        print("\n请安装依赖:")
        print("pip install sentence-transformers")
        return False

def download_model_huggingface(model_name, output_dir):
    """从 Hugging Face 下载模型"""
    from sentence_transformers import SentenceTransformer

    print(f"📥 从 Hugging Face 下载模型: {model_name}")

    try:
        # 下载模型
        model = SentenceTransformer(model_name)

        # 保存模型
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        model.save(str(output_path))

        print(f"✅ 模型保存到: {output_path}")

        # 显示模型信息
        print("\n📊 模型信息:")
        print(f"  - 维度: {model.get_sentence_embedding_dimension()}")
        print(f"  - 最大长度: {model.max_seq_length}")

        # 测试
        print("\n🧪 测试模型...")
        test_text = "这是一个测试句子"
        embedding = model.encode(test_text)
        print(f"✅ 模型工作正常")
        print(f"  输入: {test_text}")
        print(f"  输出维度: {len(embedding)}")

        return True

    except Exception as e:
        print(f"❌ 下载失败: {e}")
        return False

def download_model_modelscope(model_name, output_dir):
    """从魔搭社区下载模型"""
    try:
        from modelscope import snapshot_download
        from sentence_transformers import SentenceTransformer
    except ImportError as e:
        print(f"❌ 缺少依赖: {e}")
        print("安装: pip install modelscope sentence-transformers")
        return False

    print(f"📥 从魔搭社区下载模型: {model_name}")

    try:
        # 1. 使用魔搭社区下载模型到临时目录
        temp_dir = snapshot_download(model_name)
        print(f"✅ 模型下载到缓存: {temp_dir}")

        # 2. 使用 sentence-transformers 加载并保存到目标目录
        print(f"📦 转换并保存模型到: {output_dir}")
        model = SentenceTransformer(temp_dir)

        # 保存到目标目录
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        model.save(str(output_path))

        print(f"✅ 模型保存到: {output_path}")

        # 显示模型信息
        print("\n📊 模型信息:")
        print(f"  - 维度: {model.get_sentence_embedding_dimension()}")
        print(f"  - 最大长度: {model.max_seq_length}")

        # 测试
        print("\n🧪 测试模型...")
        test_text = "这是一个测试句子"
        embedding = model.encode(test_text)
        print(f"✅ 模型工作正常")
        print(f"  输入: {test_text}")
        print(f"  输出维度: {len(embedding)}")

        return True

    except Exception as e:
        print(f"❌ 下载失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def convert_to_onnx(model_path):
    """
    将 Sentence-Transformers 模型转换为 ONNX 格式

    Args:
        model_path: 模型路径

    Returns:
        bool: 转换是否成功
    """
    print("\n🔄 转换为 ONNX 格式...")

    try:
        from sentence_transformers import SentenceTransformer
        import torch
        import subprocess
        import shutil

        # 方法1: 尝试使用 optimum-cli（更完整）
        print("💡 方法1: 尝试使用 optimum-cli...")
        output_dir = str(Path(model_path).parent / (Path(model_path).name + "-onnx"))

        # 首先检查模型是否有正确的 Hugging Face 结构
        model = SentenceTransformer(str(model_path))

        # 获取第一个模块（Transformer）
        if len(model) > 0 and hasattr(model[0], 'auto_model'):
            transformer_model = model[0].auto_model
            tokenizer = model[0].tokenizer

            # 使用 transformers 模型导出
            print("📦 使用 Transformer 模型直接导出...")

            result = subprocess.run([
                sys.executable, "-m", "optimum.exporters.onnx",
                "--model", str(model_path),
                output_dir
            ], capture_output=True, text=True)

            if result.returncode == 0:
                print("✅ optimum-cli 转换成功")
            else:
                print(f"⚠️ optimum-cli 失败: {result.stderr[:200]}")
                print("\n💡 方法2: 使用 torch.onnx.export（更稳定）...")

                # 方法2: 使用 torch 直接导出
                Path(output_dir).mkdir(parents=True, exist_ok=True)

                # 创建示例输入
                dummy_text = "This is a sample sentence"
                encoded = tokenizer(
                    dummy_text,
                    padding=True,
                    truncation=True,
                    max_length=512,
                    return_tensors="pt"
                )

                # 导出 ONNX
                onnx_path = Path(output_dir) / "model.onnx"
                torch.onnx.export(
                    transformer_model,
                    (encoded['input_ids'], encoded['attention_mask']),
                    str(onnx_path),
                    input_names=['input_ids', 'attention_mask'],
                    output_names=['last_hidden_state'],
                    dynamic_axes={
                        'input_ids': {0: 'batch', 1: 'sequence'},
                        'attention_mask': {0: 'batch', 1: 'sequence'},
                        'last_hidden_state': {0: 'batch', 1: 'sequence'}
                    },
                    opset_version=14,
                    do_constant_folding=True
                )
                print("✅ torch.onnx.export 转换成功")

        # 复制 ONNX 文件到原目录
        print("\n📋 复制 ONNX 文件到模型目录...")
        onnx_file = Path(output_dir) / "model.onnx"
        onnx_data = Path(output_dir) / "model.onnx_data"

        if onnx_file.exists():
            shutil.copy2(onnx_file, Path(model_path) / "model.onnx")
            print(f"✅ 已复制: model.onnx ({onnx_file.stat().st_size / (1024*1024):.1f} MB)")

            if onnx_data.exists():
                shutil.copy2(onnx_data, Path(model_path) / "model.onnx_data")
                print(f"✅ 已复制: model.onnx_data ({onnx_data.stat().st_size / (1024*1024):.1f} MB)")
        else:
            print("❌ 未找到 ONNX 文件")
            return False

        # 验证 ONNX 模型
        print("\n🧪 验证 ONNX 模型...")
        try:
            import onnxruntime as ort
            session = ort.InferenceSession(str(Path(model_path) / "model.onnx"))
            print("✅ ONNX 模型验证成功")

            print("\n📋 模型信息:")
            print(f"  输入:")
            for input_meta in session.get_inputs():
                print(f"    - {input_meta.name}: {input_meta.shape}")
            print(f"  输出:")
            for output_meta in session.get_outputs():
                print(f"    - {output_meta.name}: {output_meta.shape}")

        except Exception as e:
            print(f"⚠️ 验证失败: {e}")
            return False

        return True

    except Exception as e:
        print(f"❌ 转换失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    parser = argparse.ArgumentParser(
        description="下载国产向量嵌入模型"
    )
    parser.add_argument(
        "--model",
        type=str,
        required=True,
        choices=["bge-m3", "bge-large-zh", "bge-base-zh", "text2vec-base", "text2vec-large"],
        help="选择模型"
    )
    parser.add_argument(
        "--output",
        type=str,
        default="./models",
        help="输出目录（默认：./models）"
    )
    parser.add_argument(
        "--mirror",
        action="store_true",
        help="使用魔搭社区镜像（国内快）"
    )
    parser.add_argument(
        "--convert-onnx",
        action="store_true",
        default=True,
        help="自动转换为 ONNX 格式（默认启用）"
    )
    parser.add_argument(
        "--no-convert-onnx",
        dest="convert_onnx",
        action="store_false",
        help="不转换为 ONNX 格式"
    )

    args = parser.parse_args()

    # 模型映射
    model_map_hf = {
        "bge-m3": "BAAI/bge-m3",
        "bge-large-zh": "BAAI/bge-large-zh",
        "bge-base-zh": "BAAI/bge-base-zh-v1.5",
        "text2vec-base": "shibing624/text2vec-base-chinese",
        "text2vec-large": "GanymedeNil/text2vec-large-chinese"
    }

    model_map_ms = {
        "bge-m3": "Xorbits/bge-m3",
        "bge-large-zh": "AI-ModelScope/bge-large-zh",
        "bge-base-zh": "AI-ModelScope/bge-base-zh-v1.5",
        "text2vec-base": "damo/nlp_corom_sentence-embedding_chinese-base",
        "text2vec-large": "damo/nlp_corom_sentence-embedding_chinese-large"
    }

    print("=" * 70)
    print("🇨🇳 国产向量嵌入模型下载工具")
    print("=" * 70)
    print()

    # 设置镜像
    if args.mirror:
        print("🌏 使用魔搭社区镜像...")
        os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

    # 检查依赖
    if not check_dependencies():
        sys.exit(1)

    # 确定输出路径
    model_output = Path(args.output) / args.model

    # 下载模型
    if args.mirror:
        model_name = model_map_ms.get(args.model)
        success = download_model_modelscope(model_name, str(model_output))
    else:
        model_name = model_map_hf.get(args.model)
        success = download_model_huggingface(model_name, str(model_output))

    if success:
        # 自动转换为 ONNX（如果启用）
        if args.convert_onnx:
            onnx_success = convert_to_onnx(str(model_output))
            if not onnx_success:
                print("\n⚠️ ONNX 转换失败，但 PyTorch 模型已下载")
                print("💡 可以稍后手动转换:")
                print(f"   python {sys.argv[0]} --model {args.model} --convert-onnx")

        print("\n" + "=" * 70)
        print("🎉 完成！")
        print("=" * 70)
        print()
        print("📝 下一步：")
        print("1. 更新 application.yml 配置")
        print(f"   model:")
        print(f"     name: {args.model}")
        print(f"     path: {model_output}/model.onnx")
        print()
        print("2. 重建向量索引")
        print("   访问: http://localhost:8080")
        print("   点击: 重建索引")
        print()
        print("3. 测试检索效果")
        print("   对比新旧模型的检索准确率")
    else:
        print("\n❌ 模型下载失败")
        print("\n💡 故障排查:")
        print("1. 检查网络连接")
        print("2. 尝试使用镜像: --mirror")
        print("3. 手动下载:")
        if args.mirror:
            print(f"   访问: https://modelscope.cn/models/{model_map_ms.get(args.model)}")
        else:
            print(f"   访问: https://huggingface.co/{model_map_hf.get(args.model)}")
        sys.exit(1)

if __name__ == "__main__":
    main()

