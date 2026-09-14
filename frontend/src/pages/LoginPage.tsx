import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { useAuth } from '@/hooks/useAuth'
import { errorMessage, isApiRequestError } from '@/utils/errors'


const schema = z.object({
  email: z.string().trim().email('Enter a valid email'),
  password: z.string().min(1, 'Password is required').max(72, 'Password is too long'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  async function onSubmit(values: FormValues) {
    try {
      await login(values)
      navigate(from, { replace: true })
    } catch (error) {
      form.setError('root', { message: errorMessage(error, 'Unable to log in.') })
      if (isApiRequestError(error)) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'email' || fieldError.field === 'password') {
            form.setError(fieldError.field, { message: fieldError.message })
          }
        }
      }
    }
  }

  return (
    <AuthShell title="Welcome back" subtitle="Pick up where you left off — your tray is waiting.">
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
        {form.formState.errors.root?.message ? <Alert>{form.formState.errors.root.message}</Alert> : null}
        <Field label="Email" type="email" autoComplete="email" {...form.register('email')} error={form.formState.errors.email?.message} />
        <Field
          label="Password"
          type="password"
          autoComplete="current-password"
          {...form.register('password')}
          error={form.formState.errors.password?.message}
        />
        <div className="flex justify-end">
          <Link to="/forgot-password" className="text-sm font-semibold text-saffron hover:text-saffron-dark">
            Forgot password?
          </Link>
        </div>
        <Button type="submit" pending={form.formState.isSubmitting} className="w-full">
          Log in
        </Button>
      </form>
      <p className="mt-6 text-sm text-ink/65">
        New here?{' '}
        <Link to="/register" className="font-semibold text-saffron">
          Create an account
        </Link>
      </p>
    </AuthShell>
  )
}
