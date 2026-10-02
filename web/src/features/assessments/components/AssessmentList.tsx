import type { Assessment, Page } from '../types/assessment'
import { formatDate } from '../types/formatDate'
import styles from '../pages/AssessmentsPage.module.css'

type Props = {
  data: Page<Assessment> | null
  loading: boolean
  page: number
  onPageChange: (page: number) => void
  onOpen: (id: string) => void
  onCreate: () => void
}

export function AssessmentList({ data, loading, page, onPageChange, onOpen, onCreate }: Props) {
  return (
    <section className={styles.panel}>
      <div className={styles.listHead}>
        <h2>All assessments</h2>
        <p>{data?.totalElements ?? 0} total assessments</p>
      </div>
      {loading ? (
        <p className={styles.empty}>Loading assessments…</p>
      ) : data?.content.length ? (
        <>
          <div className={styles.tableWrap}>
            <table>
              <thead>
                <tr>
                  <th>ASSESSMENT</th>
                  <th>STATUS</th>
                  <th>CREATED</th>
                  <th>UPDATED</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <button className={styles.titleButton} onClick={() => onOpen(item.id)}>
                        {item.title}
                      </button>
                      <small>{item.description || 'No description'}</small>
                    </td>
                    <td>
                      <span className={styles.status}>{item.status}</span>
                    </td>
                    <td>{formatDate(item.createdAt)}</td>
                    <td>{formatDate(item.updatedAt)}</td>
                    <td>
                      <button
                        className={styles.arrow}
                        aria-label={`Open ${item.title}`}
                        onClick={() => onOpen(item.id)}
                      >
                        ↗
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className={styles.pagination}>
            <span>
              Page {page + 1} of {data.totalPages}
            </span>
            <div>
              <button
                className="buttonSecondary"
                disabled={data.first}
                onClick={() => onPageChange(page - 1)}
              >
                Previous
              </button>
              <button
                className="buttonSecondary"
                disabled={data.last}
                onClick={() => onPageChange(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </>
      ) : (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>▦</div>
          <h3>No assessments yet</h3>
          <p>Create your first assessment to get started.</p>
          <button onClick={onCreate}>Create assessment</button>
        </div>
      )}
    </section>
  )
}
