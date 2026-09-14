import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { complaintApi } from '@/api/complaintApi'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { SelectField } from '@/components/ui/SelectField'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'
import type { ComplaintPriority, ComplaintStatus } from '@/types/complaint'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'


export function ComplaintDetailPage() {
  const { complaintId } = useParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const staff = Boolean(user?.roles.some((role) => role === 'STAFF' || role === 'ADMIN'))
  const [uploadError, setUploadError] = useState<string | null>(null)

  const query = useQuery({
    queryKey: ['complaints', complaintId],
    queryFn: () => complaintApi.get(complaintId!),
    enabled: Boolean(complaintId),
    retry: false,
  })

  const imagesQuery = useQuery({
    queryKey: ['complaints', complaintId, 'images'],
    queryFn: () => complaintApi.images(complaintId!),
    enabled: Boolean(complaintId),
    retry: false,
  })

  const upload = useMutation({
    mutationFn: (file: File) => complaintApi.uploadImage(complaintId!, file),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['complaints', complaintId] })
      void queryClient.invalidateQueries({ queryKey: ['complaints', complaintId, 'images'] })
    },
  })

  const removeImage = useMutation({
    mutationFn: (imageId: string) => complaintApi.deleteImage(complaintId!, imageId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['complaints', complaintId, 'images'] })
    },
  })

  const update = useMutation({
    mutationFn: (body: { status?: ComplaintStatus; priority?: ComplaintPriority }) =>
      complaintApi.update(complaintId!, body),
    onSuccess: (complaint) => queryClient.setQueryData(['complaints', complaintId], complaint),
  })

  if (query.isLoading) {
    return <Spinner label="Loading complaint…" />
  }
  if (query.isError || !query.data) {
    return <Alert>{errorMessage(query.error)}</Alert>
  }

  const complaint = query.data
  const images = imagesQuery.data ?? complaint.images ?? []

  return (
    <article className="space-y-6">
      <div>
        <p className="text-xs uppercase text-ink/50">{formatEnum(complaint.priority)} priority</p>
        <h1 className="font-display text-3xl">
          {formatEnum(complaint.category)} · {formatEnum(complaint.status)}
        </h1>
        <p className="text-sm text-ink/55">{formatDateTime(complaint.createdAt)}</p>
      </div>
      {!staff ? (
        <Alert tone="info">An executive will call you soon using the email on your account.</Alert>
      ) : null}
      <p className="rounded-2xl border border-ink/10 bg-white p-4 text-ink/80">{complaint.description}</p>

      {staff ? (
        <form
          className="grid gap-3 sm:grid-cols-2"
          onSubmit={(event) => {
            event.preventDefault()
            const form = new FormData(event.currentTarget)
            update.mutate({
              status: form.get('status') as ComplaintStatus,
              priority: form.get('priority') as ComplaintPriority,
            })
          }}
        >
          <SelectField label="Status" name="status" defaultValue={complaint.status}>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="REJECTED">Rejected</option>
          </SelectField>
          <SelectField label="Priority" name="priority" defaultValue={complaint.priority}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </SelectField>
          {update.isError ? <Alert>{errorMessage(update.error)}</Alert> : null}
          <Button type="submit" pending={update.isPending} className="sm:col-span-2">
            Update ticket
          </Button>
        </form>
      ) : null}

      <section className="space-y-3">
        <h2 className="font-semibold">Photos</h2>
        <p className="text-sm text-ink/60">
          Add a photo of the order if it helps us see what went wrong.
        </p>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={async (event) => {
            const file = event.target.files?.[0]
            event.target.value = ''
            if (!file) {
              return
            }
            setUploadError(null)
            try {
              await upload.mutateAsync(file)
            } catch (error) {
              setUploadError(errorMessage(error, 'Unable to upload the image.'))
            }
          }}
        />
        {uploadError ? <Alert>{uploadError}</Alert> : null}
        {images.length === 0 ? <Alert tone="info">No images yet.</Alert> : null}
        <ul className="grid gap-3 sm:grid-cols-3">
          {images.map((image) => (
            <li key={image.id} className="overflow-hidden rounded-xl border border-ink/10 bg-white">
              {image.url ? <img src={image.url} alt={image.fileName} className="aspect-square w-full object-cover" /> : null}
              <button
                type="button"
                className="px-3 py-2 text-xs text-saffron"
                onClick={() => removeImage.mutate(image.id)}
              >
                Delete
              </button>
            </li>
          ))}
        </ul>
      </section>
    </article>
  )
}
