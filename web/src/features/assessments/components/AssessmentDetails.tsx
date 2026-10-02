import type { Assessment } from '../types/assessment'
import { formatDate } from '../types/formatDate'
import styles from '../pages/AssessmentsPage.module.css'

type Props = {
  assessment: Assessment
  busy: boolean
  onBack: () => void
  onEdit: () => void
  onDelete: () => void
}

export function AssessmentDetails({ assessment, busy, onBack, onEdit, onDelete }: Props) {
  return (
    <section className={styles.panel}>
      <button className={styles.back} onClick={onBack}>
        ← All assessments
      </button>
      <div className={styles.detailTitle}>
        <div>
          <span className={styles.status}>{assessment.status}</span>
          <h2>{assessment.title}</h2>
        </div>
        <div className={styles.detailActions}>
          <button className="buttonSecondary" onClick={onEdit}>
            Edit
          </button>
          <button className="buttonDanger" disabled={busy} onClick={onDelete}>
            Delete
          </button>
        </div>
      </div>
      <p className={styles.description}>{assessment.description || 'No description added yet.'}</p>
      <div className={styles.metadata}>
        <div>
          <small>CREATED</small>
          <strong>{formatDate(assessment.createdAt)}</strong>
        </div>
        <div>
          <small>LAST UPDATED</small>
          <strong>{formatDate(assessment.updatedAt)}</strong>
        </div>
        <div>
          <small>ASSESSMENT ID</small>
          <strong className={styles.id}>{assessment.id}</strong>
        </div>
      </div>
    </section>
  )
}
