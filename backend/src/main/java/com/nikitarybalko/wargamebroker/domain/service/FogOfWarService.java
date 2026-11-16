package com.nikitarybalko.wargamebroker.domain.service;

import com.nikitarybalko.wargamebroker.domain.model.UnitState;

import java.util.List;

/**
 * Інтерфейс обчислювального ядра розрахунку просторової видимості (Туман війни).
 * Повністю ізольований від фреймворків, БД та контролерів.
 */
public interface FogOfWarService {

    /**
     * Розраховує перелік ворожих юнітів, видимих для хоча б одного дієздатного союзника.
     *
     * @param allies  список союзних юнітів (спостерігачі)
     * @param enemies список ворожих юнітів (потенційні цілі)
     * @return список ворожих юнітів, що потрапляють у зону огляду союзних сил
     */
    List<UnitState> calculateVisibleEnemies(List<UnitState> allies, List<UnitState> enemies);

    /**
     * Перевіряє, чи бачить конкретний спостерігач цільового юніта в неперервному 2D-просторі.
     *
     * @param observer юніт-спостерігач
     * @param target   цільовий юніт
     * @return true, якщо відстань між юнітами не перевищує ефективного радіусу огляду спостерігача
     */
    boolean isVisible(UnitState observer, UnitState target);
}
