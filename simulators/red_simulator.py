#!/usr/bin/env python3
import argparse
import time
import sys
from mesa_model import TacticalWargameModel

def create_red_model(broker_url: str, api_key: str) -> TacticalWargameModel:
    model = TacticalWargameModel(side="RED", api_key=api_key, broker_url=broker_url)
    # Eastern deployment flank
    model.add_tactical_unit("red-tank-1", x=280.0, y=75.0, view_radius=60.0, speed=3.8)
    model.add_tactical_unit("red-scout-1", x=265.0, y=95.0, view_radius=85.0, speed=5.5)
    model.add_tactical_unit("red-infantry-1", x=285.0, y=115.0, view_radius=50.0, speed=2.5)
    model.add_tactical_unit("red-apc-1", x=275.0, y=85.0, view_radius=55.0, speed=4.2)
    return model

def run_red_simulator(broker_url: str, api_key: str, tick_delay: float, max_ticks: int):
    model = create_red_model(broker_url, api_key)
    if not model.wait_for_broker() or not model.register():
        return

    model.log(f"Starting Mesa simulation run for {max_ticks} ticks...")
    while model.current_tick <= max_ticks:
        lifecycle = model.sync_simulation_lifecycle()
        if lifecycle == "INITIALIZING":
            model.log("Simulation session is initializing. Waiting...")
            time.sleep(1.0)
            continue
        elif lifecycle == "PAUSED":
            model.log("Simulation session is paused. Waiting for resume...")
            time.sleep(1.0)
            continue
        elif lifecycle == "STOPPED":
            model.log("Simulation session stopped.")
            break

        model.step()
        time.sleep(tick_delay)

    model.log("Red Force Mesa simulation finished.")

def main():
    parser = argparse.ArgumentParser(description="Red Force Mesa Simulator")
    parser.add_argument("--url", default="http://localhost:8080", help="Wargame Broker base URL")
    parser.add_argument("--api-key", default="red-secret-token", help="Red force API key")
    parser.add_argument("--delay", type=float, default=0.5, help="Delay between ticks in seconds")
    parser.add_argument("--ticks", type=int, default=50, help="Max ticks to simulate")
    args = parser.parse_args()

    run_red_simulator(args.url, args.api_key, args.delay, args.ticks)

if __name__ == "__main__":
    main()
