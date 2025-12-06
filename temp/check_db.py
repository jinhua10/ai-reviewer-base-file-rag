import sqlite3
import os

db_path = r'D:\Jetbrains\hackathon\ai-reviewer-base-file-rag\data\rag\metadata\metadata.db'
docs_path = r'D:\Jetbrains\hackathon\ai-reviewer-base-file-rag\data\documents'

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# 检查表是否存在
cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
tables = cursor.fetchall()
print(f'数据库中的表: {tables}')

# 查询数据库中的记录数
try:
    cursor.execute('SELECT COUNT(*) FROM documents')
    db_count = cursor.fetchone()[0]
    print(f'数据库中的文档记录数: {db_count}')
except Exception as e:
    print(f'查询失败: {e}')
    db_count = 0

# 查询所有文档的文件路径
cursor.execute('SELECT id, title, file_path FROM documents')
db_docs = cursor.fetchall()

# 检查文件系统中实际存在的文件
missing_files = []
for doc_id, title, file_path in db_docs:
    full_path = os.path.join(r'D:\Jetbrains\hackathon\ai-reviewer-base-file-rag\data\rag\documents', file_path)
    if not os.path.exists(full_path):
        missing_files.append((doc_id, title, file_path))

print(f'\n数据库中有记录但文件不存在的文档数: {len(missing_files)}')
if missing_files:
    print('\n前10个缺失的文件:')
    for doc_id, title, file_path in missing_files[:10]:
        print(f'  ID: {doc_id[:20]}..., Title: {title}, Path: {file_path}')

# 统计文件系统中的文件数
file_count = 0
if os.path.exists(docs_path):
    for filename in os.listdir(docs_path):
        if os.path.isfile(os.path.join(docs_path, filename)):
            file_count += 1

print(f'\n文件系统中的实际文件数: {file_count}')
print(f'差异: {db_count - file_count} (数据库记录数 - 文件数)')

conn.close()

