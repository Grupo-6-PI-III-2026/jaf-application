import { ReactNode } from "react";
import styles from "./ChartCard.module.css";

interface ChartCardProps {
  title: string;
  children: ReactNode;
  right?: ReactNode;
}

export default function ChartCard({ title, children, right }: ChartCardProps) {
  return (
    <div className={styles.chartCard}>
      <div className={styles.chartCardHeader}>
        <h3 className={styles.chartCardTitle}>{title}</h3>
        {right}
      </div>
      {children}
    </div>
  );
}
