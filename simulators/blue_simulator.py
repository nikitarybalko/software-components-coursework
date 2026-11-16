#!/usr/bin/env python3
import argparse
import time
from typing import Optional

from mesa_model import TacticalWargameModel
from unit_profiles import UnitType

def create_blue_model(broker_url: str, api_key: str, seed: Optional[int] = None) -> TacticalWargameModel:
    model = TacticalWargameModel(side="BLUE", api_key=api_key, broker_url=broker_url, seed=seed)
    # Western deployment flank
    model.add_tactical_unit("blue-tank-1", UnitType.ARMOR, x=20.0, y=70.0)
    model.add_tactical_unit("blue-scout-1", UnitType.RECON, x=35.0, y=100.0)
    model.add_tactical_unit("blue-infantry-1", UnitType.INFANTRY, x=15.0, y=120.0)
    model.add_tactical_unit("blue-apc-1", UnitType.MECHANIZED_INFANTRY, x=25.0, y=90.0)
    return model

def run_blue_simulator(broker_url: str, api_key: str, tick_delay: float, max_ticks: int,
                       seed: Optional[int] = None):
    model = create_blue_model(broker_url, api_key, seed)
    if not model.wait_for_broker() or not model.register():
        return

    model.log(f"Starting Mesa simulation run for {max_ticks} ticks...")
    while model.current_tick <= max_ticks:
        lifecycle = model.sync_simulation_lifecycle()
        if model.current_tick > max_ticks:
            break
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

    model.log("Blue Force Mesa simulation finished.")

def main():
    parser = argparse.ArgumentParser(description="Blue Force Mesa Simulator")
    parser.add_argument("--url", default="http://localhost:8080", help="Wargame Broker base URL")
    parser.add_argument("--api-key", default="blue-secret-token", help="Blue force API key")
    parser.add_argument("--delay", type=float, default=0.5, help="Delay between ticks in seconds")
    parser.add_argument("--ticks", type=int, default=50, help="Max ticks to simulate")
    parser.add_argument("--seed", type=int, default=None, help="Random seed for a reproducible run")
    args = parser.parse_args()

    run_blue_simulator(args.url, args.api_key, args.delay, args.ticks, args.seed)

if __name__ == "__main__":
    main()
