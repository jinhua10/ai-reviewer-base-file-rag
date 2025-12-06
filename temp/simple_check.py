import sqlite3

db_path = r'D:\Jetbrains\hackathon\ai-reviewer-base-file-rag\data\rag\metadata\metadata.db'

try:
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # 检查表
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = cursor.fetchall()
    print(f'Tables: {tables}')

    # 查询记录数
    cursor.execute('SELECT COUNT(*) FROM documents')
    count = cursor.fetchone()[0]
    print(f'Document count in DB: {count}')

    # 查询所有ID
    cursor.execute('SELECT id, title FROM documents LIMIT 5')
    rows = cursor.fetchall()
    print(f'\nFirst 5 records:')
    for row in rows:
        print(f'  {row[0]}: {row[1]}')

    conn.close()
except Exception as e:
    print(f'Error: {e}')
    import traceback
    traceback.print_exc()

