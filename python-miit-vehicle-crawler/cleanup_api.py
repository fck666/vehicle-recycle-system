import requests

BACKEND = "http://localhost:8090"

def get_token():
    print("Logging in...")
    resp = requests.post(f"{BACKEND}/api/auth/login", json={"username": "admin", "password": "password"})
    if not resp.ok:
        print(f"Login failed: {resp.text}")
        return None
    return resp.json()["token"]

def cleanup():
    token = get_token()
    if not token:
        return
        
    headers = {"Authorization": f"Bearer {token}"}
    # 1. Search all vehicles
    print("Searching vehicles...")
    resp = requests.get(f"{BACKEND}/api/admin/vehicles?size=100", headers=headers)
    if not resp.ok:
        print(f"Error listing vehicles: {resp.text}")
        return
    
    data = resp.json()
    items = data.get("content", [])
    print(f"Found {len(items)} vehicles.")
    
    # 2. Delete each
    for item in items:
        vid = item["id"]
        print(f"Deleting vehicle {vid}...")
        r = requests.delete(f"{BACKEND}/api/admin/vehicles/{vid}", headers=headers)
        if r.ok:
            print("Deleted.")
        else:
            print(f"Failed: {r.text}")

    print("Done.")

if __name__ == "__main__":
    cleanup()
