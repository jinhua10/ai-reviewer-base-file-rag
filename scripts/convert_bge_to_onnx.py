#!/usr/bin/env python3
"""
BGE 模型 ONNX 转换脚本
使用 optimum 的 ORTModelForFeatureExtraction 进行转换
"""

import sys
import shutil
from pathlib import Path

def main():
    print("=" * 60)
    print("🔄 BGE 模型 ONNX 转换工具")
    print("=" * 60)

    model_path = Path("models/bge-base-zh")
    output_dir = Path("models/bge-base-zh-onnx-temp")

    if not model_path.exists():
        print(f"❌ 模型目录不存在: {model_path}")
        return 1

    print(f"\n📁 源模型: {model_path}")
    print(f"📁 临时输出: {output_dir}")

    # 检查当前 model.onnx 大小
    current_onnx = model_path / "model.onnx"
    if current_onnx.exists():
        size_mb = current_onnx.stat().st_size / (1024 * 1024)
        print(f"\n⚠️  当前 model.onnx 大小: {size_mb:.2f} MB")
        if size_mb < 10:
            print("   这个文件太小，需要重新转换")

    print("\n🔄 开始转换...")

    try:
        from optimum.onnxruntime import ORTModelForFeatureExtraction

        # 清理旧的临时目录
        if output_dir.exists():
            shutil.rmtree(output_dir)

        output_dir.mkdir(parents=True, exist_ok=True)

        # 转换模型
        print("   加载模型并转换为 ONNX...")
        ort_model = ORTModelForFeatureExtraction.from_pretrained(
            str(model_path),
            export=True
        )
        ort_model.save_pretrained(str(output_dir))
        print("   ✅ 转换完成")

        # 检查生成的文件
        print("\n📄 生成的文件:")
        total_size = 0
        for f in sorted(output_dir.iterdir()):
            size_mb = f.stat().st_size / (1024 * 1024)
            total_size += size_mb
            print(f"   {f.name}: {size_mb:.2f} MB")
        print(f"   总大小: {total_size:.2f} MB")

        # 复制文件到目标目录
        print("\n📋 复制文件到模型目录...")

        onnx_file = output_dir / "model.onnx"
        onnx_data = output_dir / "model.onnx_data"

        if onnx_file.exists():
            # 备份旧文件
            if current_onnx.exists():
                backup = model_path / "model.onnx.bak"
                shutil.move(str(current_onnx), str(backup))
                print(f"   已备份旧文件到 model.onnx.bak")

            shutil.copy2(str(onnx_file), str(model_path / "model.onnx"))
            new_size = (model_path / "model.onnx").stat().st_size / (1024 * 1024)
            print(f"   ✅ model.onnx ({new_size:.2f} MB)")

        if onnx_data.exists():
            shutil.copy2(str(onnx_data), str(model_path / "model.onnx_data"))
            data_size = (model_path / "model.onnx_data").stat().st_size / (1024 * 1024)
            print(f"   ✅ model.onnx_data ({data_size:.2f} MB)")

        # 清理临时目录
        print("\n🧹 清理临时文件...")
        shutil.rmtree(output_dir)
        print("   ✅ 已删除临时目录")

        # 验证
        print("\n🧪 验证模型...")
        final_onnx = model_path / "model.onnx"
        final_size = final_onnx.stat().st_size / (1024 * 1024)

        if final_size < 10:
            print(f"   ❌ 转换后文件仍然太小 ({final_size:.2f} MB)")
            return 1

        try:
            import onnxruntime as ort
            sess = ort.InferenceSession(str(final_onnx), providers=['CPUExecutionProvider'])
            print(f"   ✅ ONNX Runtime 加载成功")
            print(f"   输入: {[i.name for i in sess.get_inputs()]}")
            print(f"   输出: {[o.name for o in sess.get_outputs()]}")
        except Exception as e:
            print(f"   ⚠️ ONNX Runtime 验证失败: {e}")

        print("\n" + "=" * 60)
        print("✅ 转换完成!")
        print("=" * 60)
        return 0

    except ImportError as e:
        print(f"\n❌ 缺少依赖: {e}")
        print("请安装: pip install optimum[onnxruntime]")
        return 1
    except Exception as e:
        print(f"\n❌ 转换失败: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == "__main__":
    sys.exit(main())

