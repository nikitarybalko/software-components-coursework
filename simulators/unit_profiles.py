from dataclasses import dataclass
from enum import Enum
from typing import Optional


class UnitType(str, Enum):
    """Combat function of a unit; values match the broker's UnitType enum."""
    ARMOR = "ARMOR"
    MECHANIZED_INFANTRY = "MECHANIZED_INFANTRY"
    INFANTRY = "INFANTRY"
    RECON = "RECON"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class UnitProfile:
    view_radius: float
    """Distance at which the unit spots enemies."""
    speed: float
    """Distance covered per tick."""
    weapon_range: float
    """Enemies closer than this are under the unit's fire."""
    firepower: float
    """Chance per tick to hit one enemy inside the weapon range."""
    protection: float
    """Share of incoming hits absorbed by armour or cover (0 = none, 1 = invulnerable)."""
    engagement_distance: float
    """Distance the unit tries to keep from the nearest enemy while healthy."""


UNIT_PROFILES = {
    UnitType.ARMOR: UnitProfile(
        view_radius=60.0, speed=3.8, weapon_range=25.0, firepower=0.12, protection=0.5, engagement_distance=21.0),
    UnitType.MECHANIZED_INFANTRY: UnitProfile(
        view_radius=55.0, speed=4.2, weapon_range=18.0, firepower=0.08, protection=0.3, engagement_distance=15.0),
    UnitType.INFANTRY: UnitProfile(
        view_radius=50.0, speed=2.5, weapon_range=15.0, firepower=0.07, protection=0.0, engagement_distance=12.0),
    UnitType.RECON: UnitProfile(
        view_radius=85.0, speed=5.5, weapon_range=12.0, firepower=0.03, protection=0.1, engagement_distance=45.0),
    UnitType.UNKNOWN: UnitProfile(
        view_radius=50.0, speed=3.0, weapon_range=15.0, firepower=0.05, protection=0.0, engagement_distance=12.0),
}

DAMAGED_SPEED_FACTOR = 0.5
DAMAGED_FIREPOWER_FACTOR = 0.5
WITHDRAWAL_MARGIN = 5.0
"""How far beyond the nearest threat's weapon range a damaged unit pulls back."""


def profile_of(unit_type: Optional[str]) -> UnitProfile:
    """Profile for a type reported by the broker; unknown or missing types fall back to UNKNOWN."""
    try:
        return UNIT_PROFILES[UnitType(unit_type)]
    except ValueError:
        return UNIT_PROFILES[UnitType.UNKNOWN]
