import { Link, useLocation } from 'react-router-dom'


export function BrandPage() {
  const { pathname } = useLocation()
  const slug = pathname.replace(/^\//, '')

  if (slug === 'about') {
    return <AboutPage />
  }
  if (slug === 'story') {
    return <StoryPage />
  }

  const page = SHORT[slug] ?? SHORT.about
  return (
    <section className="mx-auto max-w-2xl space-y-5">
      <Eyebrow />
      <h1 className="font-display text-4xl">{page.title}</h1>
      <p className="text-lg leading-relaxed text-ink/75">{page.body}</p>
      <MenuLink />
      {slug === 'contact' ? (
        <Link to="/help" className="block text-sm font-semibold text-ink/70">
          Help
        </Link>
      ) : null}
    </section>
  )
}

const SHORT: Record<string, { title: string; body: string }> = {
  about: {
    title: 'About us',
    body: 'Samosa Junction is a modern Indian snack kitchen.',
  },
  careers: {
    title: 'Careers',
    body: 'We are a small kitchen first. When we hire for cooking, delivery, or guest care, roles will be listed here. There are no openings today. You can still write to us through Help.',
  },
  privacy: {
    title: 'Privacy',
    body: 'We collect your name, email, delivery address, order history, wallet movements, support messages, and complaint details so we can take orders and help you. Passwords are stored hashed. We do not sell your data. Product images may be stored as files. You can ask us to close an account by sending a support message. This notice describes the current product; it is not a substitute for a lawyer-reviewed policy if we operate as a registered company.',
  },
  terms: {
    title: 'Terms',
    body: 'By creating an account you agree to order from the live menu, pay the total confirmed at checkout, and cancel only while the kitchen still allows it. Wallet add-money and test-card checkout are simulators for this product — they do not move money at a bank. Nutrition figures are as listed on each product. Complaints apply to paid orders. We may refuse orders that we cannot fulfil. Indian law applies to disputes if this kitchen becomes a registered business.',
  },
  contact: {
    title: 'Contact us',
    body: 'For how-to questions, open Help guides. For an account question, send a support message. For a problem with a paid order, file a complaint. There is no phone desk yet.',
  },
}

function AboutPage() {
  return (
    <article className="mx-auto max-w-2xl space-y-8">
      <header className="space-y-4">
        <Eyebrow />
        <h1 className="font-display text-4xl md:text-5xl">About Samosa Junction</h1>
        <p className="text-xs font-semibold tracking-[0.16em] text-saffron uppercase">
          The samosa we love. Reimagined for the way we eat today.
        </p>
      </header>

      <p className="text-xl leading-relaxed text-ink">
        Samosa is one of India’s most loved snacks. Crispy, comforting, affordable, and almost impossible to say no to.
      </p>
      <p className="text-lg leading-relaxed text-ink/75">
        But somewhere along the way, many of us started saying no. Not because we stopped loving samosas — but because we
        started caring more about what goes into them.
      </p>
      <p className="text-lg leading-relaxed text-ink/75">
        Conventional snack options are often deep-fried, calorie-dense, light on nutrition details, and hard to fit into a
        health-conscious day. The classic samosa didn’t always match how we wanted to eat.
      </p>

      <p className="font-display text-2xl text-ink">So we decided to rethink it.</p>
      <p className="text-lg leading-relaxed text-ink/75">
        Welcome to Samosa Junction — a modern Indian snack kitchen built around a simple idea:
      </p>
      <blockquote className="border-l-4 border-saffron pl-5 font-display text-2xl leading-snug text-ink">
        Why should you have to choose between the snack you love and the way you want to eat?
      </blockquote>

      <p className="text-lg leading-relaxed text-ink/75">We took the samosa we all know and started experimenting.</p>
      <ul className="grid gap-2 text-ink sm:grid-cols-2">
        <Idea>Oven-baked shells</Idea>
        <Idea>Millet-based options</Idea>
        <Idea>Paneer and lentil fillings</Idea>
        <Idea>Protein-packed recipes</Idea>
        <Idea>Clear nutrition information</Idea>
      </ul>
      <p className="text-lg leading-relaxed text-ink/75">
        The goal isn’t to change what makes a samosa a samosa. It’s to reimagine it for today — including the classic
        potato-and-pea favourite, still on the tray.
      </p>
      <MenuLink />
    </article>
  )
}

function StoryPage() {
  return (
    <article className="mx-auto max-w-2xl space-y-8">
      <header className="space-y-4">
        <Eyebrow />
        <h1 className="font-display text-4xl md:text-5xl">Our story</h1>
      </header>
      <p className="text-lg leading-relaxed text-ink/75">It started with a simple question:</p>
      <blockquote className="border-l-4 border-saffron pl-5 font-display text-2xl leading-snug text-ink">
        Can India’s favourite snack become a little more thoughtful?
      </blockquote>
      <p className="text-lg leading-relaxed text-ink/75">
        The samosa has been part of Indian snack culture for generations. It’s the snack we grab with chai, share with
        friends, order when we’re hungry, and somehow always find room for.
      </p>
      <p className="text-lg leading-relaxed text-ink/75">
        But we noticed something. A growing number of people were setting it aside — not because they didn’t crave it, but
        because they wanted better choices: lighter cooking methods, more protein, and nutrition they could actually read.
      </p>
      <p className="text-lg leading-relaxed text-ink/75">
        So we went back to the beginning. We tried different shells, fillings, and cooking methods. We put baked samosas,
        millet shells, and paneer-lentil fillings on the same tray as the classic.
      </p>
      <p className="font-display text-2xl text-ink">And that’s how Samosa Junction was born.</p>
      <p className="text-lg leading-relaxed text-ink/75">
        The same love for the samosa. A different way of making it. From the potato-and-pea favourite to baked, millet and
        protein-rich creations, we’re building a snack experience that’s craveable, transparent, and made for everyday
        life.
      </p>
      <p className="font-display text-2xl leading-snug text-ink">
        Because the best snack isn’t the one you have to feel guilty about. It’s the one you can’t wait to have again.
      </p>
      <MenuLink />
    </article>
  )
}

function Eyebrow() {
  return <p className="text-xs font-semibold tracking-[0.2em] text-saffron uppercase">Samosa Junction</p>
}

function Idea({ children }: { children: string }) {
  return <li className="rounded-2xl bg-white px-4 py-3 text-sm font-medium shadow-sm">{children}</li>
}

function MenuLink() {
  return (
    <Link to="/#menu" className="inline-flex text-sm font-semibold text-saffron">
      See the menu
    </Link>
  )
}
