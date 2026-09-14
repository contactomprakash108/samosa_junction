import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { authApi } from '@/api/authApi'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { errorMessage, isApiRequestError } from '@/utils/errors'


const schema = z.object({
  email: z.string().trim().email('Enter a valid email').max(320),
})

type FormValues = z.infer<typeof schema>

export function ForgotPasswordPage() {
  const [result, setResult] = useState<{ message: string; resetPath?: string } | null>(null)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '' },
  })

  async function onSubmit(values: FormValues) {
    try {
      const response = await authApi.forgotPassword(values)
      setResult({ message: response.message, resetPath: response.resetPath })
    } catch (error) {
      form.setError('root', { message: errorMessage(error, 'Unable to start a password reset.') })
      if (isApiRequestError(error)) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'email') {
            form.setError('email', { message: fieldError.message })
          }
        }
      }
    }
  }

  return (
    <AuthShell
      title="Forgot your password?"
      subtitle="Enter the email on your account. We’ll only send a reset if we recognise it."
    >
      {result ? (
        <div className="space-y-4">
          <Alert tone="success">{result.message}</Alert>
          {result.resetPath ? (
            <p className="text-sm text-ink/70">You can set a new password now.</p>
          ) : (
            <p className="text-sm text-ink/70">If an account exists, check your inbox for the next step.</p>
          )}
          {result.resetPath ? (
            <Link
              to={result.resetPath}
              className="inline-flex w-full items-center justify-center rounded-full bg-saffron px-5 py-2.5 text-sm font-semibold text-cream hover:bg-saffron-dark"
            >
              Continue to reset password
            </Link>
          ) : null}
          <Link to="/login" className="block text-center text-sm font-semibold text-saffron">
            Back to log in
          </Link>
        </div>
      ) : (
        <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          {form.formState.errors.root?.message ? <Alert>{form.formState.errors.root.message}</Alert> : null}
          <Field
            label="Email"
            type="email"
            autoComplete="email"
            {...form.register('email')}
            error={form.formState.errors.email?.message}
          />
          <Button type="submit" pending={form.formState.isSubmitting} className="w-full">
            Send reset link
          </Button>
          <p className="text-center text-sm text-ink/65">
            Remembered it?{' '}
            <Link to="/login" className="font-semibold text-saffron">
              Log in
            </Link>
          </p>
        </form>
      )}
    </AuthShell>
  )
}
