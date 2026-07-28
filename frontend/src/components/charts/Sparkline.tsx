interface SparklineProps {
  points: number[];
  width?: number;
  height?: number;
}

/**
 * 12-punktowy trend w kolorze wyciszonym; bieżący okres w akcencie.
 * Linia 2px, końcowy znacznik r=3.5 z 2px pierścieniem w kolorze powierzchni.
 */
export function Sparkline({ points, width = 88, height = 28 }: SparklineProps) {
  if (points.length < 2) return null;

  const padding = 5;
  const min = Math.min(...points);
  const max = Math.max(...points);
  const span = max - min || 1;

  const x = (index: number) =>
    padding + (index * (width - padding * 2)) / (points.length - 1);
  const y = (value: number) =>
    height - padding - ((value - min) / span) * (height - padding * 2);

  const path = points
    .map((value, index) => `${index === 0 ? "M" : "L"}${x(index)} ${y(value)}`)
    .join(" ");

  const lastIndex = points.length - 1;

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      aria-hidden="true"
      className="shrink-0 overflow-visible"
    >
      <path
        d={path}
        fill="none"
        stroke="#b4b7bd"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle
        cx={x(lastIndex)}
        cy={y(points[lastIndex])}
        r={3.5}
        fill="var(--color-accent)"
        stroke="var(--color-surface)"
        strokeWidth={2}
      />
    </svg>
  );
}
