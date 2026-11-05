export type Side = "BLUE" | "RED";

export type UnitStatus = "HEALTHY" | "DAMAGED" | "DESTROYED";

export type SimulationStatus = "INITIALIZING" | "ACTIVE" | "PAUSED" | "STOPPED";

export type EventType =
  | "UNIT_DESTROYED"
  | "UNIT_DAMAGED"
  | "VALIDATION_ERROR"
  | "TIMEOUT_DISCONNECT"
  | "SIMULATOR_REGISTERED"
  | "SIMULATION_INITIALIZED"
  | "SIMULATION_PAUSED"
  | "SIMULATION_RESUMED"
  | "SIMULATION_RESET"
  | "SNAPSHOT_SAVED"
  | "SNAPSHOT_RESTORED";

export interface Unit {
  id: string;
  side: Side;
  x: number;
  y: number;
  viewRadius: number;
  effectiveViewRadius: number;
  status: UnitStatus;
}

export interface Battlefield {
  currentTick: number;
  status: SimulationStatus;
  blueUnits: Unit[];
  redUnits: Unit[];
  blueVisibleRed: Unit[];
  redVisibleBlue: Unit[];
}

export interface SimulationStatusInfo {
  currentTick: number;
  status: SimulationStatus;
  tickComplete: boolean;
  registeredSides: Side[];
  submittedSides: Side[];
}

export interface SnapshotInfo {
  id: string;
  tickNumber: number;
  createdAt: string;
  units: Unit[];
}

export interface EventLogEntry {
  id: string;
  timestamp: string;
  type: EventType;
  side: Side | null;
  message: string;
}

export interface Point {
  x: number;
  y: number;
}
