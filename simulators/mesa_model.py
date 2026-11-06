import math
import time
import requests
from typing import List, Dict, Optional, Tuple

import mesa

from unit_profiles import (DAMAGED_SPEED_FACTOR, DAMAGED_FIREPOWER_FACTOR, UNIT_PROFILES,
                           WITHDRAWAL_MARGIN, UnitProfile, UnitType, profile_of)


class TacticalUnitAgent(mesa.Agent):
    """
    Tactical combat unit agent operating in Mesa ContinuousSpace.
    Characteristics come from the profile of its unit type.
    """
    def __init__(self, model: "TacticalWargameModel", unit_id: str, side: str, unit_type: UnitType):
        super().__init__(model)
        self.unit_id = unit_id
        self.side = side
        self.unit_type = unit_type
        self.profile = UNIT_PROFILES[unit_type]
        self.status = "HEALTHY"  # HEALTHY, DAMAGED, DESTROYED

    def to_dict(self) -> Dict:
        x, y = self.pos if self.pos is not None else (0.0, 0.0)
        return {
            "id": self.unit_id,
            "side": self.side,
            "unitType": self.unit_type.value,
            "x": round(float(x), 2),
            "y": round(float(y), 2),
            "viewRadius": round(float(self.profile.view_radius), 2),
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

        threats = self._active_threats()
        self._resolve_incoming_fire(threats)
        if self.status == "DESTROYED":
            return

        speed = self.profile.speed * (DAMAGED_SPEED_FACTOR if self.status == "DAMAGED" else 1.0)
        if threats:
            nearest = min(threats, key=lambda e: self.distance_to(e["x"], e["y"]))
            self._hold_distance(nearest, self._desired_distance(nearest), speed)
        else:
            self._march(speed)

    def _active_threats(self) -> List[Dict]:
        """Spotted enemies that can still fight; wrecks stay visible but are neither targets nor threats."""
        return [enemy for enemy in self.model.visible_enemies if enemy.get("status") != "DESTROYED"]

    def _resolve_incoming_fire(self, threats: List[Dict]):
        """Every enemy that has this unit inside its weapon range gets one shot per tick."""
        for enemy in threats:
            enemy_profile = profile_of(enemy.get("unitType"))
            if self.distance_to(enemy["x"], enemy["y"]) > enemy_profile.weapon_range:
                continue
            if self.model.random.random() < self._hit_chance(enemy, enemy_profile):
                self._take_hit()
                if self.status == "DESTROYED":
                    return

    def _hit_chance(self, enemy: Dict, enemy_profile: UnitProfile) -> float:
        firepower = enemy_profile.firepower
        if enemy.get("status") == "DAMAGED":
            firepower *= DAMAGED_FIREPOWER_FACTOR
        return firepower * (1.0 - self.profile.protection)

    def _take_hit(self):
        self.status = "DAMAGED" if self.status == "HEALTHY" else "DESTROYED"

    def _desired_distance(self, nearest: Dict) -> float:
        """Healthy units keep their engagement distance; damaged ones pull out of the enemy's weapon range."""
        if self.status == "DAMAGED":
            enemy_range = profile_of(nearest.get("unitType")).weapon_range
            return max(self.profile.engagement_distance, enemy_range + WITHDRAWAL_MARGIN)
        return self.profile.engagement_distance

    def _hold_distance(self, enemy: Dict, distance: float, speed: float):
        """Moves towards the point at the given distance from the enemy: approaches or backs off as needed."""
        current = self.distance_to(enemy["x"], enemy["y"])
        if current < 0.001:
            return
        away_x = (self.pos[0] - enemy["x"]) / current
        away_y = (self.pos[1] - enemy["y"]) / current
        self._move_towards(enemy["x"] + away_x * distance, enemy["y"] + away_y * distance, speed)

    def _march(self, speed: float):
        direction = 1.0 if self.side == "BLUE" else -1.0
        target_x = self.pos[0] + direction * speed * 1.5
        target_y = self.pos[1] + self.model.random.uniform(-1.0, 1.0)
        self._move_towards(target_x, target_y, speed)

    def _move_towards(self, target_x: float, target_y: float, speed: float):
        dist = self.distance_to(target_x, target_y)
        if dist < 0.001:
            return

        step = min(speed, dist)
        curr_x, curr_y = self.pos
        next_x = curr_x + (target_x - curr_x) / dist * step
        next_y = curr_y + (target_y - curr_y) / dist * step

        # Clamp within ContinuousSpace boundaries
        clamped_x = max(2.0, min(self.model.space.x_max - 2.0, next_x))
        clamped_y = max(2.0, min(self.model.space.y_max - 2.0, next_y))

        self.model.space.move_agent(self, (clamped_x, clamped_y))


class TacticalWargameModel(mesa.Model):
    """
    Mesa simulation model orchestrating combat units and synchronizing
    via REST API with the Wargame Broker.
    """
    def __init__(self, side: str, api_key: str, broker_url: str = "http://localhost:8080",
                 width: float = 300.0, height: float = 200.0, seed: Optional[int] = None):
        super().__init__(seed=seed)
        self.side = side
        self.api_key = api_key
        self.broker_url = broker_url.rstrip("/")
        self.space = mesa.space.ContinuousSpace(x_max=width, y_max=height, torus=False)
        self.visible_enemies: List[Dict] = []
        self.current_tick: int = 1
        self.submitted_current_tick: bool = False
        self.state_epoch: Optional[int] = None
        self.initial_deployment: Dict[str, Tuple[float, float]] = {}

        self.session = requests.Session()
        self.session.headers.update({
            "X-API-KEY": self.api_key,
            "Content-Type": "application/json"
        })

    def log(self, message: str):
        print(f"[{self.side} FORCE] {message}", flush=True)

    def add_tactical_unit(self, unit_id: str, unit_type: UnitType, x: float, y: float):
        unit = TacticalUnitAgent(self, unit_id=unit_id, side=self.side, unit_type=unit_type)
        self.space.place_agent(unit, (x, y))
        self.initial_deployment[unit_id] = (x, y)
        return unit

    def tactical_units(self) -> List[TacticalUnitAgent]:
        return [agent for agent in self.agents if isinstance(agent, TacticalUnitAgent)]

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
                self._sync_state_epoch(data.get("epoch"))
                return data.get("status")
        except requests.RequestException as e:
            self.log(f"Error checking lifecycle status: {e}")
        return None

    def _sync_state_epoch(self, epoch: Optional[int]):
        if epoch is None or epoch == self.state_epoch:
            return
        if self.state_epoch is None or self.resync_units_with_broker():
            self.state_epoch = epoch

    def resync_units_with_broker(self) -> bool:
        """
        Replaces local agent states with the broker's view after init, reset or snapshot restore.
        An empty unit list means a new battle, so agents return to their initial deployment.
        """
        try:
            res = self.session.get(f"{self.broker_url}/api/v1/simulation/ticks/current/units", timeout=3)
            if res.status_code != 200:
                self.log(f"State resync failed ({res.status_code}): {res.text}")
                return False
            units = res.json().get("units", [])
        except requests.RequestException as e:
            self.log(f"State resync error: {e}")
            return False

        self.visible_enemies = []
        if units:
            self._apply_broker_units({unit["id"]: unit for unit in units})
            self.log(f"Broker state changed: restored {len(units)} units from snapshot.")
        else:
            self._redeploy_initial_positions()
            self.log("Broker state changed: new battle, units returned to initial deployment.")
        return True

    def _apply_broker_units(self, units_by_id: Dict[str, Dict]):
        for agent in self.tactical_units():
            state = units_by_id.get(agent.unit_id)
            if state is not None:
                self.space.move_agent(agent, (float(state["x"]), float(state["y"])))
                agent.status = state["status"]

    def _redeploy_initial_positions(self):
        for agent in self.tactical_units():
            self.space.move_agent(agent, self.initial_deployment[agent.unit_id])
            agent.status = "HEALTHY"

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
        for agent in self.tactical_units():
            agent.step()

        # 2. Package and submit current tick payload
        unit_payloads = [agent.to_dict() for agent in self.tactical_units()]

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
