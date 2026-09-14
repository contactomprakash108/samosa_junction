import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { AuthShell } from '@/components/auth/AuthShell'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { useAuth } from '@/hooks/useAuth'
import { errorMessage, isApiRequestError } from '@/utils/errors'


const schema = z.object({
  fullName: z.string().trim().min(1, 'Name is required').max(120, 'Name is too long'),
  email: z.string().trim().email('Enter a valid email').max(320),
  password: z.string().min(8, 'Use at least 8 characters').max(72, 'Password is too long'),
})

type FormValues = z.infer<typeof schema>

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', password: '' },
  })

  async function onSubmit(values: FormValues) {
    try {
      await register(values)
      navigate('/', { replace: true })
    } catch (error) {
      form.setError('root', { message: errorMessage(error, 'Unable to create the account.') })
      if (isApiRequestError(error)) {
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'email' || fieldError.field === 'password' || fieldError.field === 'fullName') {
            form.setError(fieldError.field, { message: fieldError.message })
          }
        }
      }
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Create an account to order, keep a wallet, and track every samosa."
    >
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
        {form.formState.errors.root?.message ? <Alert>{form.formState.errors.root.message}</Alert> : null}
        <Field label="Full name" autoComplete="name" {...form.register('fullName')} error={form.formState.errors.fullName?.message} />
        <Field label="Email" type="email" autoComplete="email" {...form.register('email')} error={form.formState.errors.email?.message} />
        <Field
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="At least 8 characters."
          {...form.register('password')}
          error={form.formState.errors.password?.message}
        />
        <Button type="submit" pending={form.formState.isSubmitting} className="w-full">
          Create account
        </Button>
      </form>
      <p className="mt-6 text-sm text-ink/65">
        Already registered?{' '}
        <Link to="/login" className="font-semibold text-saffron">
          Log in
        </Link>
      </p>
    </AuthShell>
  )
}
