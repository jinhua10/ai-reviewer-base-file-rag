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

def check_and_install_dependencies():
    """检查并自动安装所有必需的依赖"""
    print("=" * 70)
    print("📦 检查并安装依赖...")
    print("=" * 70)

    required_packages = {
        "transformers": "transformers>=4.30.0",
        "optimum": "optimum[onnxruntime]>=1.14.0",
        "onnxruntime": "onnxruntime>=1.15.0",
        "torch": "torch>=2.0.0",
        "onnxscript": "onnxscript>=0.1.0"
    }

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

        # 验证模型完整性
        if not validate_onnx_model(output_path):
            return False

        return True
    else:
        print(f"\n❌ 转换失败")
        return False


def validate_onnx_model(model_path):
    """
    验证 ONNX 模型文件的完整性

    Args:
        model_path: 模型目录路径

    Returns:
        bool: 验证是否通过
    """
    print("\n" + "=" * 70)
    print("🔍 验证模型完整性...")
    print("=" * 70)

    model_path = Path(model_path)
    errors = []
    warnings = []

    # 1. 检查必需文件
    required_files = {
        "model.onnx": "ONNX 模型文件",
        "tokenizer.json": "Tokenizer 文件"
    }

    optional_files = {
        "model.onnx_data": "模型权重数据（大模型需要）",
        "tokenizer_config.json": "Tokenizer 配置",
        "config.json": "模型配置",
        "special_tokens_map.json": "特殊 Token 映射"
    }

    print("\n📋 检查必需文件:")
    for filename, desc in required_files.items():
        filepath = model_path / filename
        if filepath.exists():
            size_mb = filepath.stat().st_size / (1024 * 1024)
            print(f"  ✅ {filename} ({size_mb:.2f} MB) - {desc}")
        else:
            errors.append(f"缺失必需文件: {filename} ({desc})")
            print(f"  ❌ {filename} - {desc} [缺失]")

    print("\n📋 检查可选文件:")
    for filename, desc in optional_files.items():
        filepath = model_path / filename
        if filepath.exists():
            size_mb = filepath.stat().st_size / (1024 * 1024)
            print(f"  ✅ {filename} ({size_mb:.2f} MB) - {desc}")
        else:
            print(f"  ⚠️  {filename} - {desc} [缺失]")

    # 2. 验证 model.onnx 文件大小
    onnx_file = model_path / "model.onnx"
    onnx_data_file = model_path / "model.onnx_data"

    if onnx_file.exists():
        onnx_size = onnx_file.stat().st_size
        onnx_size_mb = onnx_size / (1024 * 1024)

        print(f"\n📊 模型文件分析:")
        print(f"  - model.onnx 大小: {onnx_size_mb:.2f} MB")

        # 检查 model.onnx_data 文件
        if onnx_data_file.exists():
            data_size_mb = onnx_data_file.stat().st_size / (1024 * 1024)
            print(f"  - model.onnx_data 大小: {data_size_mb:.2f} MB")
            total_size_mb = onnx_size_mb + data_size_mb
            print(f"  - 总模型大小: {total_size_mb:.2f} MB")

            # Qwen 0.5B 模型应该至少 500MB，1.5B 至少 1.5GB
            if total_size_mb < 200:
                warnings.append(f"模型总大小仅 {total_size_mb:.1f}MB，对于 Qwen 模型来说可能不完整")
        else:
            # 检查 model.onnx 是否引用了外部数据
            has_external_ref = False
            try:
                with open(onnx_file, 'rb') as f:
                    content = f.read(50000)  # 读取前50KB
                    if b'model.onnx_data' in content or b'onnx_data' in content or b'external_data' in content:
                        has_external_ref = True
            except Exception as e:
                warnings.append(f"无法检查 model.onnx 内容: {e}")

            if has_external_ref:
                errors.append(
                    f"model.onnx 引用了外部数据文件 model.onnx_data，但该文件不存在！\n"
                    f"   这会导致模型加载失败。请重新下载模型。"
                )
                print(f"  ❌ 检测到外部数据引用，但 model.onnx_data 文件缺失!")
            elif onnx_size_mb < 100:
                # Qwen 模型即使是最小的 0.5B 版本也应该很大
                errors.append(
                    f"model.onnx 仅 {onnx_size_mb:.2f}MB，这对于 Qwen 模型来说太小了！\n"
                    f"   预期大小: Qwen-0.5B ~1GB, Qwen-1.5B ~3GB\n"
                    f"   问题: model.onnx_data 权重文件缺失，模型不完整"
                )
                print(f"  ❌ model.onnx 仅 {onnx_size_mb:.2f}MB - 模型不完整，缺少权重数据!")
            elif onnx_size_mb < 500:
                warnings.append(
                    f"model.onnx 仅 {onnx_size_mb:.1f}MB，没有 model.onnx_data 文件。\n"
                    f"   Qwen 模型通常需要 500MB+，可能不完整。"
                )
                print(f"  ⚠️  model.onnx 大小偏小，可能缺少权重数据")
            else:
                print(f"  - 模型为内联权重格式（权重嵌入在 .onnx 文件中）")

    # 3. 尝试使用 ONNX Runtime 验证
    print("\n🧪 使用 ONNX Runtime 验证模型...")
    try:
        import onnxruntime as ort

        sess_options = ort.SessionOptions()
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_DISABLE_ALL

        session = ort.InferenceSession(
            str(onnx_file),
            sess_options=sess_options,
            providers=['CPUExecutionProvider']
        )

        print("  ✅ ONNX Runtime 加载成功")
        print(f"\n📋 模型结构:")
        print(f"  输入:")
        for inp in session.get_inputs():
            print(f"    - {inp.name}: {inp.shape} ({inp.type})")
        print(f"  输出:")
        for out in session.get_outputs():
            print(f"    - {out.name}: {out.shape} ({out.type})")

    except Exception as e:
        error_msg = str(e)
        if "model.onnx_data" in error_msg or "external data" in error_msg.lower():
            errors.append(
                f"ONNX Runtime 加载失败: 缺失外部数据文件 model.onnx_data\n"
                f"   原始错误: {error_msg[:200]}"
            )
        else:
            errors.append(f"ONNX Runtime 加载失败: {error_msg[:300]}")
        print(f"  ❌ ONNX Runtime 验证失败: {error_msg[:200]}")

    # 4. 输出验证结果
    print("\n" + "=" * 70)
    if errors:
        print("❌ 模型验证失败!")
        print("\n错误列表:")
        for i, error in enumerate(errors, 1):
            print(f"  {i}. {error}")
        print("\n💡 建议:")
        print("  1. 删除当前模型目录，重新运行下载脚本")
        print("  2. 检查磁盘空间是否充足")
        print("  3. 检查网络连接是否稳定")
        print("  4. 尝试使用 --mirror 参数使用国内镜像")
        return False
    elif warnings:
        print("⚠️  模型验证通过（有警告）")
        print("\n警告列表:")
        for i, warning in enumerate(warnings, 1):
            print(f"  {i}. {warning}")
        return True
    else:
        print("✅ 模型验证通过!")
        return True

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

    # 检查并安装依赖
    if not check_and_install_dependencies():
        sys.exit(1)

    # 设置镜像
    if args.mirror:
        print("🌏 使用国内镜像...")
        os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

    # 再次验证 optimum-cli 可用性
    if not check_optimum_cli():
        print("❌ optimum-cli 仍不可用，请检查安装")
        sys.exit(1)

    print("✅ optimum-cli 已就绪\n")

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

