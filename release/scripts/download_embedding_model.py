#!/usr/bin/env python3
"""
国产向量嵌入模型下载脚本
支持 BGE、Text2Vec 等国产模型

使用方法：
    python scripts\download_embedding_model.py --model bge-m3
    python scripts\download_embedding_model.py --model bge-base-zh --mirror
"""

import os
import sys
import argparse
import subprocess
from pathlib import Path

def install_package(package_name):
    """安装 Python 包"""
    print(f"📥 安装 {package_name}...")
    try:
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "--upgrade", package_name],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.PIPE
        )
        print(f"✅ {package_name} 安装成功")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ {package_name} 安装失败: {e.stderr.decode() if e.stderr else str(e)}")
        return False

def check_dependencies(use_mirror=False):
    """检查并自动安装所有必需的依赖"""
    print("=" * 70)
    print("📦 检查并安装依赖...")
    print("=" * 70)

    required_packages = {
        "sentence_transformers": "sentence-transformers>=2.0.0",
        "torch": "torch>=2.0.0",
        "transformers": "transformers>=4.30.0",
        "optimum": "optimum[onnxruntime]>=1.14.0",
        "onnxruntime": "onnxruntime>=1.15.0",
        "onnxscript": "onnxscript>=0.1.0"
    }

    # 如果使用镜像，添加 modelscope
    if use_mirror:
        required_packages["modelscope"] = "modelscope>=1.0.0"

    installed_packages = []
    failed_packages = []

    for package_name, package_spec in required_packages.items():
        try:
            # 尝试导入包
            __import__(package_name)
            print(f"✅ {package_name} 已安装")
            installed_packages.append(package_name)
        except ImportError:
            print(f"⚠️  {package_name} 未安装，开始安装...")
            if install_package(package_spec):
                installed_packages.append(package_name)
            else:
                failed_packages.append(package_name)

    print()
    if failed_packages:
        print(f"❌ 以下依赖安装失败: {', '.join(failed_packages)}")
        print("\n请手动安装:")
        print(f"pip install {' '.join([required_packages[p] for p in failed_packages])}")
        return False

    print(f"✅ 所有依赖已就绪 ({len(installed_packages)}/{len(required_packages)})")
    print("=" * 70)
    print()
    return True

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


def merge_onnx_external_data(source_onnx_path, target_onnx_path):
    """
    将 ONNX 模型的外部数据合并到单个文件中

    Args:
        source_onnx_path: 源 ONNX 文件路径（带有外部数据）
        target_onnx_path: 目标 ONNX 文件路径（合并后的单文件）

    Returns:
        Path: 成功返回目标文件路径，失败返回 None
    """
    try:
        import onnx

        source_path = Path(source_onnx_path)
        target_path = Path(target_onnx_path)

        # 检查外部数据大小
        external_data_path = source_path.parent / "model.onnx_data"
        total_size_mb = source_path.stat().st_size / (1024 * 1024)

        if external_data_path.exists():
            external_size_mb = external_data_path.stat().st_size / (1024 * 1024)
            total_size_mb += external_size_mb
            print(f"  外部数据大小: {external_size_mb:.1f} MB")
            print(f"  总模型大小: {total_size_mb:.1f} MB")

        # Protobuf 有 2GB 限制，超过 1.9GB 的模型不建议合并
        if total_size_mb > 1900:
            print(f"  ⚠️ 模型超过 1.9GB，无法合并为单文件（Protobuf 2GB 限制）")
            print(f"  将保留分离的 model.onnx 和 model.onnx_data 文件")
            return None

        print(f"  加载模型: {source_path.name}")

        # 加载模型（包括外部数据）
        model = onnx.load(str(source_path), load_external_data=True)

        # 保存为单个文件（不使用外部数据）
        print(f"  合并到单文件: {target_path.name}")

        # 确保目标目录存在
        target_path.parent.mkdir(parents=True, exist_ok=True)

        # 保存模型，所有数据内联
        onnx.save_model(
            model,
            str(target_path),
            save_as_external_data=False  # 不使用外部数据，全部内联
        )

        return target_path

    except ImportError:
        print("  ⚠️ 需要安装 onnx 包: pip install onnx")
        return None
    except Exception as e:
        error_msg = str(e)
        if "2GB" in error_msg or "protobuf" in error_msg.lower():
            print(f"  ⚠️ 模型太大，无法合并为单文件（Protobuf 2GB 限制）")
        else:
            print(f"  ⚠️ 合并失败: {e}")
        return None


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
        import shutil

        output_dir = str(Path(model_path).parent / (Path(model_path).name + "-onnx"))
        Path(output_dir).mkdir(parents=True, exist_ok=True)

        # 方法1: 使用 optimum ORTModelForFeatureExtraction（最可靠）
        print("💡 方法1: 尝试使用 optimum ORTModelForFeatureExtraction...")
        ort_export_success = False

        try:
            from optimum.onnxruntime import ORTModelForFeatureExtraction

            ort_model = ORTModelForFeatureExtraction.from_pretrained(
                model_path,
                export=True
            )
            ort_model.save_pretrained(output_dir)
            print("✅ ORTModelForFeatureExtraction 转换成功")
            ort_export_success = True

        except Exception as e:
            print(f"  ⚠️ ORTModelForFeatureExtraction 失败: {str(e)[:150]}")

        # 方法2: 使用 optimum-cli
        if not ort_export_success:
            print("\n💡 方法2: 尝试使用 optimum-cli...")

            result = subprocess.run([
                sys.executable, "-m", "optimum.exporters.onnx",
                "--model", str(model_path),
                output_dir
            ], capture_output=True, text=True)

            if result.returncode == 0:
                print("✅ optimum-cli 转换成功")
                ort_export_success = True
            else:
                print(f"  ⚠️ optimum-cli 失败: {result.stderr[:200]}")

        # 方法3: 使用 torch.onnx.export
        if not ort_export_success:
            print("\n💡 方法3: 使用 torch.onnx.export...")

            model = SentenceTransformer(str(model_path))

            if len(model) > 0 and hasattr(model[0], 'auto_model'):
                transformer_model = model[0].auto_model
                tokenizer = model[0].tokenizer

                # 创建示例输入
                dummy_text = "This is a sample sentence"
                encoded = tokenizer(
                    dummy_text,
                    padding=True,
                    truncation=True,
                    max_length=512,
                    return_tensors="pt"
                )

                onnx_path = Path(output_dir) / "model.onnx"
                opset_versions = [11, 12, 13, 14]
                export_success = False

                transformer_model.eval()

                for opset in opset_versions:
                    try:
                        print(f"  尝试 opset_version={opset}...")
                        with torch.no_grad():
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
                                opset_version=opset,
                                do_constant_folding=True,
                                export_params=True
                            )

                        if onnx_path.exists():
                            size_mb = onnx_path.stat().st_size / (1024 * 1024)
                            if size_mb < 10:
                                print(f"  ⚠️ opset={opset}: 文件太小 ({size_mb:.1f}MB)")
                                onnx_path.unlink()
                                continue

                        print(f"✅ torch.onnx.export 转换成功 (opset={opset})")
                        export_success = True
                        break
                    except Exception as e:
                        print(f"  ⚠️ opset={opset} 失败: {str(e)[:100]}")
                        if onnx_path.exists():
                            onnx_path.unlink()
                        continue

                if not export_success:
                    print("❌ 所有转换方法都失败")
                    return False

        # 复制 ONNX 文件到原目录
        print("\n📋 复制 ONNX 文件到模型目录...")

        # 递归搜索 model.onnx 和 model.onnx_data 文件（可能在子目录中）
        output_path = Path(output_dir)
        onnx_files = list(output_path.rglob("model.onnx"))
        onnx_data_files = list(output_path.rglob("model.onnx_data"))

        print(f"  搜索到的 ONNX 文件: {[str(f.relative_to(output_path)) for f in onnx_files]}")
        if onnx_data_files:
            print(f"  搜索到的权重文件: {[str(f.relative_to(output_path)) for f in onnx_data_files]}")

        if onnx_files:
            # 优先选择最大的 model.onnx 文件（更可能是完整的）
            onnx_file = max(onnx_files, key=lambda f: f.stat().st_size)
            onnx_size_mb = onnx_file.stat().st_size / (1024 * 1024)

            # 检查是否有外部数据文件需要合并
            onnx_data = onnx_file.parent / "model.onnx_data"
            has_external_data = onnx_data.exists() or onnx_data_files

            if has_external_data:
                # 如果有外部数据，先合并再复制
                print("\n🔧 合并外部数据到 ONNX 文件...")
                merged_onnx_path = merge_onnx_external_data(onnx_file, Path(model_path) / "model.onnx")
                if merged_onnx_path:
                    merged_size_mb = merged_onnx_path.stat().st_size / (1024 * 1024)
                    print(f"✅ 已合并: model.onnx ({merged_size_mb:.1f} MB) - 包含所有权重数据")

                    # 删除旧的 model.onnx_data 文件（如果存在）
                    old_data_file = Path(model_path) / "model.onnx_data"
                    if old_data_file.exists():
                        old_data_file.unlink()
                        print(f"🧹 已删除旧的 model.onnx_data 文件")
                else:
                    # 合并失败，回退到复制两个文件
                    print("⚠️  合并失败，将分别复制 model.onnx 和 model.onnx_data")
                    shutil.copy2(onnx_file, Path(model_path) / "model.onnx")
                    print(f"✅ 已复制: model.onnx ({onnx_size_mb:.1f} MB)")

                    if onnx_data.exists():
                        data_size_mb = onnx_data.stat().st_size / (1024 * 1024)
                        shutil.copy2(onnx_data, Path(model_path) / "model.onnx_data")
                        print(f"✅ 已复制: model.onnx_data ({data_size_mb:.1f} MB)")
                    elif onnx_data_files:
                        largest_data = max(onnx_data_files, key=lambda f: f.stat().st_size)
                        data_size_mb = largest_data.stat().st_size / (1024 * 1024)
                        shutil.copy2(largest_data, Path(model_path) / "model.onnx_data")
                        print(f"✅ 已复制: model.onnx_data ({data_size_mb:.1f} MB)")
            else:
                # 没有外部数据，直接复制
                shutil.copy2(onnx_file, Path(model_path) / "model.onnx")
                print(f"✅ 已复制: model.onnx ({onnx_size_mb:.1f} MB)")

                if onnx_size_mb < 10:
                    print(f"⚠️  警告: model.onnx 仅 {onnx_size_mb:.1f}MB，未找到 model.onnx_data 文件!")
                    print(f"   这对于嵌入模型来说太小了，模型可能不完整")
        else:
            print("❌ 未找到 ONNX 文件")
            return False

        # 清理临时目录
        print("\n🧹 清理临时文件...")
        try:
            shutil.rmtree(output_dir)
            print(f"✅ 已删除临时目录: {Path(output_dir).name}")
        except Exception as e:
            print(f"⚠️ 清理临时目录失败: {e}")

        # 验证 ONNX 模型
        print("\n" + "=" * 60)
        print("🔍 验证 ONNX 模型完整性...")
        print("=" * 60)

        onnx_model_path = Path(model_path) / "model.onnx"
        onnx_data_path = Path(model_path) / "model.onnx_data"
        errors = []
        warnings = []

        # 1. 检查 model.onnx 文件是否存在
        if not onnx_model_path.exists():
            errors.append("ONNX 模型文件 (model.onnx) 不存在")
            print("❌ model.onnx 不存在")
        else:
            file_size = onnx_model_path.stat().st_size
            file_size_mb = file_size / (1024 * 1024)
            print(f"✅ model.onnx ({file_size_mb:.2f} MB)")

            if file_size < 1024:  # 小于 1KB
                errors.append(f"model.onnx 文件太小 ({file_size} bytes)，可能已损坏")

        # 2. 检查 model.onnx_data 文件（外部权重数据）
        if onnx_data_path.exists():
            data_size = onnx_data_path.stat().st_size
            data_size_mb = data_size / (1024 * 1024)
            print(f"✅ model.onnx_data ({data_size_mb:.2f} MB)")
        else:
            # 检查 model.onnx 是否引用了外部数据文件
            if onnx_model_path.exists():
                file_size_mb = onnx_model_path.stat().st_size / (1024 * 1024)

                # 检查文件内容是否引用了外部数据
                has_external_ref = False
                try:
                    with open(onnx_model_path, 'rb') as f:
                        content = f.read(100000)  # 读取前100KB检查
                        # 检查是否有外部数据引用
                        if b'model.onnx_data' in content or b'onnx_data' in content or b'external_data' in content:
                            has_external_ref = True
                except Exception as e:
                    warnings.append(f"无法检查 model.onnx 内容: {e}")

                if has_external_ref:
                    errors.append(
                        "model.onnx 引用了外部数据文件 model.onnx_data，但该文件缺失！\n"
                        "   这会导致模型加载失败（Unsupported model IR version 或 file not found 错误）"
                    )
                    print("❌ model.onnx_data 缺失（model.onnx 需要此文件！）")
                elif file_size_mb < 10:
                    # 小于 10MB 的嵌入模型几乎肯定是不完整的
                    # BGE-base-zh 约 400MB, BGE-m3 约 2GB
                    errors.append(
                        f"❌ model.onnx 仅 {file_size_mb:.2f}MB，这对于嵌入模型来说太小了！\n"
                        f"   预期大小: BGE-base-zh ~400MB, BGE-m3 ~2GB\n"
                        f"   问题: model.onnx_data 权重文件缺失，模型不完整"
                    )
                    print(f"❌ model.onnx 仅 {file_size_mb:.2f}MB - 模型不完整，缺少权重数据！")
                elif file_size_mb < 100:
                    # 10-100MB 的模型可能有问题
                    warnings.append(
                        f"model.onnx 仅 {file_size_mb:.1f}MB，可能缺少 model.onnx_data 文件。\n"
                        "   如果模型加载失败，请尝试重新下载。"
                    )
                    print(f"⚠️  model.onnx_data 不存在（模型可能不完整）")
                else:
                    print(f"ℹ️  model.onnx_data 不存在（权重已内联在 model.onnx 中）")

        # 3. 检查其他必需文件
        required_files = ["tokenizer.json", "vocab.txt"]
        found_tokenizer = False
        for req_file in required_files:
            req_path = Path(model_path) / req_file
            if req_path.exists():
                found_tokenizer = True
                print(f"✅ {req_file}")

        if not found_tokenizer:
            warnings.append("未找到 tokenizer.json 或 vocab.txt，tokenizer 可能无法正常工作")
            print("⚠️  未找到 tokenizer 文件")

        # 4. 使用 ONNX Runtime 验证
        if errors:
            print("\n❌ 跳过 ONNX Runtime 验证（存在严重错误）")
        else:
            print("\n🧪 使用 ONNX Runtime 加载验证...")
            try:
                import onnxruntime as ort

                sess_options = ort.SessionOptions()
                sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_DISABLE_ALL

                session = ort.InferenceSession(
                    str(onnx_model_path),
                    sess_options=sess_options,
                    providers=['CPUExecutionProvider']
                )
                print("✅ ONNX Runtime 加载成功")

                print("\n📋 模型结构:")
                print("  输入:")
                for input_meta in session.get_inputs():
                    print(f"    - {input_meta.name}: {input_meta.shape}")
                print("  输出:")
                for output_meta in session.get_outputs():
                    print(f"    - {output_meta.name}: {output_meta.shape}")

            except Exception as e:
                error_msg = str(e)
                if "model.onnx_data" in error_msg or "external data" in error_msg.lower() or "file_size" in error_msg.lower():
                    errors.append(
                        f"ONNX Runtime 加载失败: 缺失外部数据文件 model.onnx_data\n"
                        f"   原始错误: {error_msg[:200]}"
                    )
                elif "IR version" in error_msg:
                    errors.append(
                        f"ONNX Runtime 版本不兼容: {error_msg[:200]}\n"
                        f"   建议升级 ONNX Runtime: pip install --upgrade onnxruntime"
                    )
                else:
                    warnings.append(f"ONNX Runtime 验证失败: {error_msg[:200]}")
                print(f"⚠️  验证警告: {error_msg[:150]}")

        # 5. 输出验证结果
        print("\n" + "=" * 60)
        if errors:
            print("❌ 模型验证失败!")
            print("\n错误列表:")
            for i, error in enumerate(errors, 1):
                print(f"  {i}. {error}")
            print("\n💡 修复建议:")
            print("  1. 删除模型目录，重新运行下载脚本")
            print("  2. 确保网络连接稳定，磁盘空间充足")
            print("  3. 使用 --mirror 参数尝试国内镜像")
            print("  4. 如果问题持续，尝试较小的模型（如 bge-base-zh）")
            return False
        elif warnings:
            print("⚠️  模型验证通过（有警告）")
            print("\n警告列表:")
            for i, warning in enumerate(warnings, 1):
                print(f"  {i}. {warning}")
            print("\n模型可能可用，但如果遇到问题请参考上述警告。")
            return True
        else:
            print("✅ 模型验证完全通过!")
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

    # 检查并安装依赖
    if not check_dependencies(use_mirror=args.mirror):
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
