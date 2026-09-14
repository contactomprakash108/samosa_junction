import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { authApi } from '@/api/authApi'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { errorMessage, isApiRequestError } from '@/utils/errors'


const schema = z.object({
  token: z.string().trim().min(1, 'Reset token is required'),
  password: z.string().min(8, 'Use at least 8 characters').max(72, 'Password is too long'),
})

type FormValues = z.infer<typeof schema>

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const [done, setDone] = useState(false)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { token: params.get('token') ?? '', password: '' },
  })

  async function onSubmit(values: FormValues) {
    try {
      await authApi.resetPassword(values)
      setDone(true)
    } catch (error) {
      form.setError('root', { message: errorMessage(error, 'Unable to reset the password.') })
      if (isApiRequestError(error)) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'token' || fieldError.field === 'password') {
            form.setError(fieldError.field, { message: fieldError.message })
          }
        }
      }
    }
  }

  return (
    <AuthShell title="Choose a new password" subtitle="The token is single-use and expires in 30 minutes.">
      {done ? (
        <div className="space-y-4">
          <Alert tone="success">Password updated. You can log in with the new password.</Alert>
          <Link
            to="/login"
            className="inline-flex w-full items-center justify-center rounded-full bg-saffron px-5 py-2.5 text-sm font-semibold text-cream hover:bg-saffron-dark"
          >
            Go to log in
          </Link>
        </div>
      ) : (
        <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          {form.formState.errors.root?.message ? <Alert>{form.formState.errors.root.message}</Alert> : null}
          <Field
            label="Reset token"
            autoComplete="off"
            hint="Filled automatically from the reset link."
            {...form.register('token')}
            error={form.formState.errors.token?.message}
          />
          <Field
            label="New password"
            type="password"
            autoComplete="new-password"
            {...form.register('password')}
            error={form.formState.errors.password?.message}
          />
          <Button type="submit" pending={form.formState.isSubmitting} className="w-full">
            Update password
          </Button>
        </form>
      )}
    </AuthShell>
  )
}
