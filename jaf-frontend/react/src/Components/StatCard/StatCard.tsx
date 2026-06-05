import type { ReactNode } from "react";
import styles from "./StatCard.module.css";

interface StatCardProps {
  label: string;
  value: string;
  progress?: number;
  action?: ReactNode;
}

export default function StatCard({ label, value, progress, action }: StatCardProps) {
  return (
    <div className={styles.statCard}>
      <div className={styles.statCardHeader}>
        <div className={styles.statCardLabel}>{label}</div>
        {action}
      </div>
      <div className={styles.statCardValue}>{value}</div>
      {progress !== undefined && (
        <div className={styles.progressBar}>
          <div
            className={styles.progressFill}
            style={{ width: `${progress}%` }}
          />
        </div>
      )}
    </div>
  );
}
