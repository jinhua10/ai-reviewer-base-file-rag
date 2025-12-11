package top.yumbo.ai.rag.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地存储引擎 (Local Storage Engine)
 *
 * 负责本地文件的读写操作 (Responsible for local file I/O operations)
 * 支持 JSON 序列化和反序列化 (Supports JSON serialization/deserialization)
 * 线程安全设计 (Thread-safe design)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class LocalStorageEngine {

    /**
     * JSON 序列化工具 (JSON serialization tool)
     */
    private final ObjectMapper objectMapper;

    /**
     * 读写锁 (Read-write lock)
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // ========== 初始化 (Initialization) ==========

    public LocalStorageEngine() {
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块 (Register Java 8 time module)
        this.objectMapper.registerModule(new JavaTimeModule());

        log.info(I18N.get("local.storage.initialized"));
    }

    // ========== 文件写入 (File Write) ==========

    /**
     * 保存对象到文件 (Save object to file)
     *
     * @param object 要保存的对象 (Object to save)
     * @param filePath 文件路径 (File path)
     * @param <T> 对象类型 (Object type)
     */
    public <T> void save(T object, String filePath) {
        lock.writeLock().lock();
        try {
            // 确保父目录存在 (Ensure parent directory exists)
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            // 序列化并写入 (Serialize and write)
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), object);

            log.debug(I18N.get("local.storage.saved"), filePath);

        } catch (Exception e) {
            log.error(I18N.get("local.storage.save_failed"), filePath, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 保存多个对象到文件 (Save multiple objects to file)
     *
     * @param objects 对象列表 (Object list)
     * @param filePath 文件路径 (File path)
     * @param <T> 对象类型 (Object type)
     */
    public <T> void saveList(List<T> objects, String filePath) {
        lock.writeLock().lock();
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), objects);

            log.debug(I18N.get("local.storage.saved"), filePath);

        } catch (Exception e) {
            log.error(I18N.get("local.storage.save_failed"), filePath, e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========== 文件读取 (File Read) ==========

    /**
     * 从文件加载对象 (Load object from file)
     *
     * @param filePath 文件路径 (File path)
     * @param clazz 对象类型 (Object class)
     * @param <T> 对象类型 (Object type)
     * @return 加载的对象，文件不存在返回 null (Loaded object, null if not exists)
     */
    public <T> T load(String filePath, Class<T> clazz) {
        lock.readLock().lock();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.debug(I18N.get("local.storage.file_not_found"), filePath);
                return null;
            }

            T object = objectMapper.readValue(file, clazz);
            log.debug(I18N.get("local.storage.loaded"), filePath);

            return object;

        } catch (Exception e) {
            log.error(I18N.get("local.storage.load_failed"), filePath, e.getMessage(), e);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 从文件加载对象列表 (Load object list from file)
     *
     * @param filePath 文件路径 (File path)
     * @param clazz 对象类型 (Object class)
     * @param <T> 对象类型 (Object type)
     * @return 对象列表，文件不存在返回空列表 (Object list, empty if not exists)
     */
    public <T> List<T> loadList(String filePath, Class<T> clazz) {
        lock.readLock().lock();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.debug(I18N.get("local.storage.file_not_found"), filePath);
                return new ArrayList<>();
            }

            var listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, clazz);
            List<T> objects = objectMapper.readValue(file, listType);

            log.debug(I18N.get("local.storage.loaded"), filePath);
            return objects;

        } catch (Exception e) {
            log.error(I18N.get("local.storage.load_failed"), filePath, e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== 文件操作 (File Operations) ==========

    /**
     * 删除文件 (Delete file)
     *
     * @param filePath 文件路径 (File path)
     * @return 是否成功 (Success or not)
     */
    public boolean delete(String filePath) {
        lock.writeLock().lock();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn(I18N.get("local.storage.file_not_found"), filePath);
                return false;
            }

            boolean deleted = file.delete();
            if (deleted) {
                log.info(I18N.get("local.storage.deleted"), filePath);
            } else {
                log.warn(I18N.get("local.storage.delete_failed"), filePath, "Unknown reason");
            }

            return deleted;

        } catch (Exception e) {
            log.error(I18N.get("local.storage.delete_failed"), filePath, e.getMessage(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查文件是否存在 (Check if file exists)
     *
     * @param filePath 文件路径 (File path)
     * @return 是否存在 (Exists or not)
     */
    public boolean exists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 获取文件大小 (Get file size)
     *
     * @param filePath 文件路径 (File path)
     * @return 文件大小（字节），不存在返回 -1 (File size in bytes, -1 if not exists)
     */
    public long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : -1;
    }

    /**
     * 复制文件 (Copy file)
     *
     * @param sourcePath 源文件路径 (Source file path)
     * @param targetPath 目标文件路径 (Target file path)
     * @return 是否成功 (Success or not)
     */
    public boolean copy(String sourcePath, String targetPath) {
        lock.writeLock().lock();
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                log.warn(I18N.get("local.storage.file_not_found"), sourcePath);
                return false;
            }

            // 确保目标目录存在 (Ensure target directory exists)
            Files.createDirectories(target.getParent());

            // 复制文件 (Copy file)
            Files.copy(source, target);
            log.info(I18N.get("local.storage.copied"), sourcePath, targetPath);

            return true;

        } catch (Exception e) {
            log.error(I18N.get("local.storage.copy_failed"), sourcePath, targetPath, e.getMessage(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移动文件 (Move file)
     *
     * @param sourcePath 源文件路径 (Source file path)
     * @param targetPath 目标文件路径 (Target file path)
     * @return 是否成功 (Success or not)
     */
    public boolean move(String sourcePath, String targetPath) {
        lock.writeLock().lock();
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                log.warn(I18N.get("local.storage.file_not_found"), sourcePath);
                return false;
            }

            // 确保目标目录存在 (Ensure target directory exists)
            Files.createDirectories(target.getParent());

            // 移动文件 (Move file)
            Files.move(source, target);
            log.info(I18N.get("local.storage.moved"), sourcePath, targetPath);

            return true;

        } catch (Exception e) {
            log.error(I18N.get("local.storage.move_failed"), sourcePath, targetPath, e.getMessage(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========== 批量操作 (Batch Operations) ==========

    /**
     * 批量保存对象 (Batch save objects)
     *
     * @param objects 对象映射（文件名 -> 对象） (Object map: filename -> object)
     * @param dirPath 目录路径 (Directory path)
     */
    public void batchSave(java.util.Map<String, Object> objects, String dirPath) {
        lock.writeLock().lock();
        try {
            // 确保目录存在 (Ensure directory exists)
            Files.createDirectories(Paths.get(dirPath));

            // 批量保存 (Batch save)
            int successCount = 0;
            for (var entry : objects.entrySet()) {
                try {
                    String filePath = dirPath + "/" + entry.getKey();
                    objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(filePath), entry.getValue());
                    successCount++;
                } catch (Exception e) {
                    log.warn(I18N.get("local.storage.save_failed"), entry.getKey(), e.getMessage());
                }
            }

            log.info(I18N.get("local.storage.batch_saved"), successCount, objects.size());

        } catch (Exception e) {
            log.error(I18N.get("local.storage.batch_save_failed"), e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

