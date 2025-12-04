#!/usr/bin/env python3
"""
Qwen ONNX 模型下载脚本（使用 optimum-cli）
最简单、最稳定的方式

使用方法：
    python download_qwen_onnx.py --model 0.5b
    或者
    python download_qwen_onnx.py --model 1.5b
"""

import os
import sys
import argparse
import subprocess
from pathlib import Path

def check_optimum_cli():
    """检查 optimum-cli 是否可用"""
    result = subprocess.run(
        [sys.executable, "-m", "optimum.exporters.onnx", "--help"],
        capture_output=True
    )
    return result.returncode == 0

def download_onnx_model(model_name, output_dir):
    """使用 optimum-cli 下载 ONNX 模型"""

    print(f"🚀 开始下载并转换模型: {model_name}")

    # 创建输出目录
    model_dir_name = model_name.split("/")[-1].lower()
    output_path = Path(output_dir) / model_dir_name
    output_path.mkdir(parents=True, exist_ok=True)

    print(f"📁 输出路径: {output_path}")

    # 使用 optimum-cli 导出
    print("🔄 转换为 ONNX 格式（这可能需要几分钟，请耐心等待）...")
    print("💡 提示：首次下载会从 Hugging Face 下载模型，速度取决于网络")

    cmd = [
        sys.executable, "-m", "optimum.exporters.onnx",
        "--model", model_name,
        "--task", "text-generation-with-past",
        str(output_path)
    ]

    print(f"\n执行命令: {' '.join(cmd)}\n")

    result = subprocess.run(cmd, text=True)

    if result.returncode == 0:
        print(f"\n✅ 模型下载和转换完成！")
        print(f"📁 模型路径: {output_path}")

        # 列出生成的文件
        print("\n📄 生成的文件:")
        for file in output_path.iterdir():
            size_mb = file.stat().st_size / (1024 * 1024)
            print(f"  - {file.name} ({size_mb:.1f} MB)")

        return True
    else:
        print(f"\n❌ 转换失败")
        return False

def main():
    parser = argparse.ArgumentParser(
        description="下载 Qwen 模型并转换为 ONNX 格式（使用 optimum-cli）"
    )
    parser.add_argument(
        "--model",
        type=str,
        default="0.5b",
        choices=["0.5b", "1.5b", "7b"],
        help="选择模型大小：0.5b（推荐）、1.5b、7b"
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
        help="使用国内镜像加速下载"
    )

    args = parser.parse_args()

    # 模型映射
    model_map = {
        "0.5b": "Qwen/Qwen2.5-0.5B-Instruct",
        "1.5b": "Qwen/Qwen2.5-1.5B-Instruct",
        "7b": "Qwen/Qwen2-7B-Instruct"
    }

    print("=" * 70)
    print("🇨🇳 Qwen ONNX 模型下载工具（使用 optimum-cli）")
    print("=" * 70)
    print()

    # 设置镜像
    if args.mirror:
        print("🌏 使用国内镜像...")
        os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

    # 检查依赖
    print("📦 检查依赖...")
    if not check_optimum_cli():
        print("❌ optimum-cli 不可用")
        print("\n请安装依赖:")
        print("pip install optimum[onnxruntime] transformers torch")
        sys.exit(1)

    print("✅ optimum-cli 可用\n")

    # 下载模型
    model_name = model_map[args.model]
    success = download_onnx_model(model_name, args.output)

    if success:
        print("\n" + "=" * 70)
        print("🎉 完成！")
        print("=" * 70)
        print()
        print("📝 下一步：")
        print("1. 检查模型文件")
        print(f"   ls {args.output}/{model_name.split('/')[-1].lower()}/")
        print()
        print("2. 更新 application.yml 配置")
        model_dir = model_name.split('/')[-1].lower()
        print(f"   model-path: ./{args.output}/{model_dir}/model.onnx")
        print(f"   tokenizer-path: ./{args.output}/{model_dir}/tokenizer.json")
        print()
        print("3. 启动应用")
        print("   ./mvnw spring-boot:run")
    else:
        print("\n❌ 模型下载失败")
        print("\n💡 故障排查:")
        print("1. 检查网络连接")
        print("2. 尝试使用镜像: --mirror")
        print("3. 检查磁盘空间（需要 2-10GB）")
        print("4. 查看错误信息并搜索解决方案")
        print("\n💡 替代方案:")
        print("使用 Ollama（更简单）:")
        print("  ollama pull qwen2.5:0.5b")
        sys.exit(1)

if __name__ == "__main__":
    main()

