import math
import random
import time
import requests
from typing import List, Dict, Optional, Tuple

import mesa


class TacticalUnitAgent(mesa.Agent):
    """
    Tactical combat unit agent operating in Mesa ContinuousSpace.
    """
    def __init__(self, model: "TacticalWargameModel", unit_id: str, side: str,
                 view_radius: float, speed: float = 3.5):
        super().__init__(model)
        self.unit_id = unit_id
        self.side = side
        self.view_radius = view_radius
        self.speed = speed
        self.status = "HEALTHY"  # HEALTHY, DAMAGED, DESTROYED

    def to_dict(self) -> Dict:
        x, y = self.pos if self.pos is not None else (0.0, 0.0)
        return {
            "id": self.unit_id,
            "side": self.side,
            "x": round(float(x), 2),
            "y": round(float(y), 2),
            "viewRadius": round(float(self.view_radius), 2),
            "status": self.status
        }

    def distance_to(self, target_x: float, target_y: float) -> float:
        if self.pos is None:
            return float("inf")
        dx = self.pos[0] - target_x
        dy = self.pos[1] - target_y
        return math.hypot(dx, dy)

    def step(self):
        if self.status == "DESTROYED" or self.pos is None:
            return

        visible_enemies = self.model.visible_enemies
        self._resolve_combat(visible_enemies)
        if self.status == "DESTROYED":
            return

        effective_speed = self.speed * 0.5 if self.status == "DAMAGED" else self.speed

        if visible_enemies:
            # Maneuver towards nearest spotted enemy
            nearest = min(visible_enemies, key=lambda e: self.distance_to(e["x"], e["y"]))
            self._move_towards(nearest["x"], nearest["y"], effective_speed, min_standoff=10.0)
        else:
            # Tactical march across the battlefield
            target_x = self.pos[0] + (effective_speed * 1.5 if self.side == "BLUE" else -effective_speed * 1.5)
            target_y = self.pos[1] + random.uniform(-1.0, 1.0)
            self._move_towards(target_x, target_y, effective_speed, min_standoff=0.0)

    def _move_towards(self, target_x: float, target_y: float, speed: float, min_standoff: float):
        dist = self.distance_to(target_x, target_y)
        if dist <= min_standoff or dist < 0.001:
            return

        step = min(speed, dist - min_standoff)
        curr_x, curr_y = self.pos
        next_x = curr_x + (target_x - curr_x) / dist * step
        next_y = curr_y + (target_y - curr_y) / dist * step

        # Clamp within ContinuousSpace boundaries
        clamped_x = max(2.0, min(self.model.space.x_max - 2.0, next_x))
        clamped_y = max(2.0, min(self.model.space.y_max - 2.0, next_y))

        self.model.space.move_agent(self, (clamped_x, clamped_y))

    def _resolve_combat(self, visible_enemies: List[Dict]):
        for enemy in visible_enemies:
            ex = enemy.get("x", 0.0)
            ey = enemy.get("y", 0.0)
            if self.distance_to(ex, ey) <= 18.0:
                roll = random.random()
                if roll < 0.15:
                    self.status = "DAMAGED"
                elif roll < 0.22 and self.status == "DAMAGED":
                    self.status = "DESTROYED"
                break


class TacticalWargameModel(mesa.Model):
    """
    Mesa simulation model orchestrating combat units and synchronizing
    via REST API with the Wargame Broker.
    """
    def __init__(self, side: str, api_key: str, broker_url: str = "http://localhost:8080",
                 width: float = 300.0, height: float = 200.0):
        super().__init__()
        self.side = side
        self.api_key = api_key
        self.broker_url = broker_url.rstrip("/")
        self.space = mesa.space.ContinuousSpace(x_max=width, y_max=height, torus=False)
        self.visible_enemies: List[Dict] = []
        self.current_tick: int = 1
        self.submitted_current_tick: bool = False

        self.session = requests.Session()
        self.session.headers.update({
            "X-API-KEY": self.api_key,
            "Content-Type": "application/json"
        })

    def log(self, message: str):
        print(f"[{self.side} FORCE] {message}", flush=True)

    def add_tactical_unit(self, unit_id: str, x: float, y: float, view_radius: float, speed: float = 3.5):
        unit = TacticalUnitAgent(self, unit_id=unit_id, side=self.side, view_radius=view_radius, speed=speed)
        self.space.place_agent(unit, (x, y))
        return unit

    def wait_for_broker(self, timeout_sec: int = 30) -> bool:
        start = time.time()
        self.log(f"Connecting to broker at {self.broker_url}...")
        while time.time() - start < timeout_sec:
            try:
                res = self.session.get(f"{self.broker_url}/api/v1/simulation/status", timeout=2)
                if res.status_code == 200:
                    self.log("Broker connection verified.")
                    return True
            except requests.RequestException:
                pass
            time.sleep(1)
        self.log("Broker connection timed out.")
        return False

    def register(self) -> bool:
        try:
            res = self.session.post(f"{self.broker_url}/api/v1/simulators/register", timeout=3)
            if res.status_code == 200:
                self.log("Registered with broker.")
                return True
            self.log(f"Registration failed ({res.status_code}): {res.text}")
        except requests.RequestException as e:
            self.log(f"Registration error: {e}")
        return False

    def sync_simulation_lifecycle(self) -> Optional[str]:
        try:
            res = self.session.get(f"{self.broker_url}/api/v1/simulation/status", timeout=3)
            if res.status_code == 200:
                data = res.json()
                self.current_tick = data.get("currentTick", self.current_tick)
                self.submitted_current_tick = self.side in data.get("submittedSides", [])
                if self.side not in data.get("registeredSides", []):
                    self.register()
                return data.get("status")
        except requests.RequestException as e:
            self.log(f"Error checking lifecycle status: {e}")
        return None

    def step(self):
        """
        Executes one simulation step:
        1. Agent decision making & movement.
        2. REST submission of tick state.
        3. Fog of war intelligence query.
        If this side already submitted the current tick, only waits for the opponent.
        """
        if self.submitted_current_tick:
            self._fetch_fog_of_war()
            return

        # 1. Step all agents in the Mesa model
        for agent in list(self.agents):
            if isinstance(agent, TacticalUnitAgent):
                agent.step()

        # 2. Package and submit current tick payload
        unit_payloads = [
            agent.to_dict()
            for agent in self.agents
            if isinstance(agent, TacticalUnitAgent)
        ]

        submit_body = {
            "tickNumber": self.current_tick,
            "units": unit_payloads
        }

        try:
            res = self.session.post(
                f"{self.broker_url}/api/v1/simulation/ticks/submit",
                json=submit_body,
                timeout=4
            )
            if res.status_code == 200:
                resp_json = res.json()
                self.submitted_current_tick = True
                self.log(f"Tick {self.current_tick} submitted. Complete: {resp_json.get('tickComplete')}")
            else:
                self.log(f"Failed to submit tick {self.current_tick} ({res.status_code}): {res.text}")
                return
        except requests.RequestException as e:
            self.log(f"Error submitting tick {self.current_tick}: {e}")
            return

        # 3. Retrieve Fog of War intelligence for current tick
        self._fetch_fog_of_war()

    def _fetch_fog_of_war(self, max_wait_sec: float = 8.0):
        start = time.time()
        while time.time() - start < max_wait_sec:
            try:
                res = self.session.get(
                    f"{self.broker_url}/api/v1/simulation/ticks/{self.current_tick}/fog-of-war?side={self.side}",
                    timeout=3
                )
                if res.status_code == 200:
                    data = res.json()
                    self.visible_enemies = data.get("visibleEnemies", [])
                    if self.visible_enemies:
                        spotted_ids = [e["id"] for e in self.visible_enemies]
                        self.log(f"Tick {self.current_tick} Intelligence: Spotted {len(self.visible_enemies)} enemies: {spotted_ids}")
                    else:
                        self.log(f"Tick {self.current_tick} Intelligence: No enemies in observation radius.")
                    return
                if res.status_code != 409:
                    self.log(f"Fog of war request failed ({res.status_code}): {res.text}")
                    return
            except requests.RequestException:
                pass
            time.sleep(0.2)
        self.log(f"Tick {self.current_tick}: opponent has not submitted yet, will keep waiting.")
