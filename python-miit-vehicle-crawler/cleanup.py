import mysql.connector
import os
import shutil

# Database config
config = {
  'user': 'root',
  'password': 'Fc20220808',
  'host': 'localhost',
  'database': 'scrap_system',
  'raise_on_warnings': True
}

try:
    print("Connecting to database...")
    cnx = mysql.connector.connect(**config)
    cursor = cnx.cursor()

    print("Cleaning up database tables...")
    # Order matters due to foreign keys if any (assuming logical cleanup)
    # vehicle_document and vehicle_image depend on vehicle_model usually
    
    tables = ['vehicle_document', 'vehicle_image', 'vehicle_model']
    for t in tables:
        try:
            cursor.execute(f"DELETE FROM {t}")
            print(f"Deleted rows from {t}")
        except Exception as e:
            print(f"Error deleting from {t}: {e}")

    cnx.commit()
    cursor.close()
    cnx.close()
    print("Database cleanup complete.")

    # File cleanup
    uploads_dir = "../backend-api/uploads/vehicles"
    if os.path.exists(uploads_dir):
        print(f"Cleaning up files in {uploads_dir}...")
        shutil.rmtree(uploads_dir)
        os.makedirs(uploads_dir, exist_ok=True)
        print("Files cleaned up.")
    else:
        print("Uploads directory not found, skipping.")

except Exception as err:
    print(f"Something went wrong: {err}")
