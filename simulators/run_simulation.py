#!/usr/bin/env python3
import argparse
import threading
import time
import requests
from blue_simulator import create_blue_model
from red_simulator import create_red_model

def init_simulation(url: str, admin_key: str) -> bool:
    print(f"[ORCHESTRATOR] Initializing simulation session on {url}...")
    headers = {"X-API-KEY": admin_key, "Content-Type": "application/json"}
    try:
        res = requests.post(f"{url}/api/v1/simulation/init", headers=headers, timeout=5)
        if res.status_code == 200:
            print("[ORCHESTRATOR] Simulation session successfully initialized and active.")
            return True
        else:
            print(f"[ORCHESTRATOR] Failed to init simulation ({res.status_code}): {res.text}")
            return False
    except requests.RequestException as e:
        print(f"[ORCHESTRATOR] Broker connection error: {e}")
        return False

def run_model_loop(model, delay: float, ticks: int):
    while model.current_tick <= ticks:
        status = model.sync_simulation_lifecycle()
        if status in ("INITIALIZING", "PAUSED"):
            time.sleep(0.5)
            continue
        elif status == "STOPPED":
            break
        model.step()
        time.sleep(delay)

def main():
    parser = argparse.ArgumentParser(description="Wargame Mesa Simulators Orchestrator")
    parser.add_argument("--url", default="http://localhost:8080", help="Wargame Broker base URL")
    parser.add_argument("--admin-key", default="admin-secret-token", help="Admin API key")
    parser.add_argument("--blue-key", default="blue-secret-token", help="Blue API key")
    parser.add_argument("--red-key", default="red-secret-token", help="Red API key")
    parser.add_argument("--delay", type=float, default=0.4, help="Delay between ticks")
    parser.add_argument("--ticks", type=int, default=30, help="Number of ticks to run")
    parser.add_argument("--no-init", action="store_true", help="Do not auto-init simulation")
    args = parser.parse_args()

    if not args.no_init:
        ready = False
        for _ in range(15):
            if init_simulation(args.url, args.admin_key):
                ready = True
                break
            time.sleep(1)
        if not ready:
            print("[ORCHESTRATOR] Broker is not reachable or initialization failed. Exiting.")
            return

    blue_model = create_blue_model(args.url, args.blue_key)
    red_model = create_red_model(args.url, args.red_key)

    if not (blue_model.register() and red_model.register()):
        print("[ORCHESTRATOR] Simulator registration failed. Exiting.")
        return

    t_blue = threading.Thread(target=run_model_loop, args=(blue_model, args.delay, args.ticks), name="BlueMesaThread")
    t_red = threading.Thread(target=run_model_loop, args=(red_model, args.delay, args.ticks), name="RedMesaThread")

    print(f"[ORCHESTRATOR] Starting Blue and Red Mesa simulators for {args.ticks} ticks...")
    t_blue.start()
    t_red.start()

    t_blue.join()
    t_red.join()
    print("[ORCHESTRATOR] Both Mesa simulators completed execution.")

if __name__ == "__main__":
    main()
