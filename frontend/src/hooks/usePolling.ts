import { useEffect, useRef } from "react";

/** Runs the task repeatedly, waiting for each run to finish before scheduling the next one. */
export function usePolling(task: () => Promise<void>, intervalMs: number) {
  const taskRef = useRef(task);
  taskRef.current = task;

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const run = async () => {
      await taskRef.current().catch(() => undefined);
      if (!cancelled) {
        timer = setTimeout(run, intervalMs);
      }
    };

    run();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [intervalMs]);
}
