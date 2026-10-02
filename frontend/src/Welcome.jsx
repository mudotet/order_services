import { useId, useRef, useState } from 'react'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { useGSAP } from '@gsap/react'
import { ArrowDown, ArrowUpRight, Cube, Package, Pause, Play, Truck } from '@phosphor-icons/react'
import '@fontsource/geist/400.css'
import '@fontsource/geist/500.css'
import '@fontsource/geist/600.css'

gsap.registerPlugin(ScrollTrigger, useGSAP)

const roles = [
  { title: 'Administrator', description: 'Manage orders, control stock and coordinate deliveries.', icon: Cube },
  { title: 'Customer', description: 'Explore products, place orders and track their journey.', icon: Package },
  { title: 'Delivery driver', description: 'View assigned orders and update delivery outcomes.', icon: Truck },
]
const statement = 'Less distraction. More clarity. So every order always has a next step.'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#d5f56b]'

function hideFailedImage(event) {
  event.currentTarget.hidden = true
}

export default function Welcome({ children }) {
  const root = useRef(null)
  const marquee = useRef(null)
  const id = useId()
  const [activeRole, setActiveRole] = useState(0)
  const [paused, setPaused] = useState(false)

  useGSAP(() => {
    const media = gsap.matchMedia()
    media.add('(prefers-reduced-motion: no-preference)', () => {
      gsap.from('[data-hero-reveal]', { y: 24, opacity: 0, duration: 1, stagger: 0.12, ease: 'power3.out' })
      gsap.fromTo('[data-word]', { opacity: 0.15 }, {
        opacity: 1, stagger: 0.12, ease: 'none',
        scrollTrigger: { trigger: '[data-desire]', start: 'top 75%', end: 'center 40%', scrub: true },
      })
      gsap.timeline({ scrollTrigger: { trigger: '[data-desire-media]', start: 'top bottom', end: 'bottom top', scrub: true } })
        .fromTo('[data-desire-image]', { scale: 0.8, opacity: 0.5 }, { scale: 1, opacity: 1, duration: 1, ease: 'none' })
        .to('[data-desire-image]', { opacity: 0.2, duration: 1, ease: 'none' })
      marquee.current = gsap.to('[data-marquee-track]', { xPercent: -50, duration: 32, repeat: -1, ease: 'none' })
      return () => { marquee.current = null }
    })
    return () => media.revert()
  }, { scope: root })

  function toggleMarquee() {
    const next = !paused
    marquee.current?.paused(next)
    setPaused(next)
  }

  function navigateRoles(event, index) {
    const keys = ['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End']
    if (!keys.includes(event.key)) return
    event.preventDefault()
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? roles.length - 1 :
      (index + (['ArrowRight', 'ArrowDown'].includes(event.key) ? 1 : -1) + roles.length) % roles.length
    event.currentTarget.closest('[data-accordion]').querySelectorAll('button')[next].focus()
    setActiveRole(next)
  }

  return <main ref={root} className="w-full max-w-full overflow-x-hidden bg-[#0b111b] text-[#f0f2ed] selection:bg-[#d5f56b] selection:text-[#0b111b]" style={{ fontFamily: 'Geist, sans-serif' }}>
    <a href="#signin" className={`sr-only focus:not-sr-only focus:fixed focus:left-6 focus:top-6 focus:z-50 focus:rounded-lg focus:bg-[#d5f56b] focus:p-4 focus:text-[#0b111b] ${focus}`}>Skip to sign in</a>
    <header className="absolute inset-x-0 top-0 z-20 px-4 pt-5 sm:px-8 sm:pt-7">
      <nav aria-label="Main navigation" className="mx-auto flex max-w-6xl items-center justify-between gap-4 rounded-full border border-white/15 bg-[#111925]/80 px-5 py-4 shadow-[0_12px_50px_#00000030] backdrop-blur-xl sm:px-7">
        <a href="#welcome" aria-label="Order Workspace, back to top" className={`flex items-center gap-2 text-sm font-semibold tracking-tight sm:text-lg ${focus}`}><span className="size-2 rounded-full bg-[#d5f56b]" aria-hidden="true" />order<span className="font-normal text-[#a8b3c3]">/ workspace</span></a>
        <a href="#features" className={`hidden text-sm text-[#c6ceda] transition-colors hover:text-[#d5f56b] sm:block ${focus}`}>Operations workspace</a>
        <a href="#signin" className={`flex shrink-0 items-center gap-2 text-xs font-medium text-[#d5f56b] sm:text-sm ${focus}`}>Sign in<ArrowUpRight size={17} aria-hidden="true" /></a>
      </nav>
    </header>

    <section id="welcome" aria-labelledby="welcome-title" className="relative isolate flex min-h-[min(900px,100svh)] flex-col items-center justify-center bg-[#172334] px-4 pb-24 pt-44 text-center sm:px-8 sm:pb-32 sm:pt-52">
      <img src="https://picsum.photos/seed/logistics-architecture/1920/1080" alt="Illustrative landscape representing space and journeys, not a product photograph" onError={hideFailedImage} fetchPriority="high" width="1920" height="1080" className="absolute inset-0 -z-20 size-full object-cover grayscale" />
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_50%_30%,#0b111b85_0%,#0b111bce_60%,#0b111b_100%)]" />
      <p data-hero-reveal className="mb-8 text-xs font-medium tracking-[0.2em] text-[#c6ceda] sm:text-sm">ORDERS GO FURTHER. OPERATIONS GO WITH THEM.</p>
      <h1 id="welcome-title" data-hero-reveal className="w-full max-w-6xl text-[clamp(1.75rem,6.8vw,6rem)] font-medium leading-[1.12] tracking-[-0.065em]">
        <span className="block whitespace-nowrap">Every order.</span><span className="block whitespace-nowrap text-[#d5f56b]">One workspace.</span>
      </h1>
      <p data-hero-reveal className="mt-8 max-w-md text-sm leading-relaxed text-[#c6ceda] sm:text-base">From products to delivery. Connect every step in one clear workspace.</p>
      <div data-hero-reveal className="mt-10 flex flex-wrap justify-center gap-3">
        <a href="#signin" className={`group flex items-center gap-5 rounded-full bg-[#d5f56b] px-7 py-4 text-sm font-semibold text-[#10170b] transition-colors hover:bg-[#e3ff95] ${focus}`}>Enter workspace<ArrowUpRight size={19} aria-hidden="true" className="transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5 motion-reduce:transform-none" /></a>
        <a href="#features" className={`flex items-center gap-5 rounded-full border border-white/30 bg-[#0b111b]/40 px-7 py-4 text-sm font-medium text-white transition-colors hover:bg-white/10 ${focus}`}>Explore<ArrowDown size={18} aria-hidden="true" /></a>
      </div>
      <div aria-hidden="true" className="absolute bottom-8 flex items-center gap-3 text-[10px] tracking-[0.18em] text-[#a8b3c3]"><span className="h-px w-8 bg-[#a8b3c3]/50" />SCROLL TO EXPLORE<span className="h-px w-8 bg-[#a8b3c3]/50" /></div>
    </section>

    <section id="features" aria-labelledby="features-title" className="mx-auto max-w-6xl scroll-mt-8 px-5 py-28 sm:px-8 md:py-40">
      <div className="mb-14 flex flex-col justify-between gap-6 md:flex-row md:items-end">
        <h2 id="features-title" className="max-w-3xl text-[clamp(2rem,4.5vw,3.75rem)] font-medium leading-[1.15] tracking-[-0.05em]">One flow.<br />Nothing disconnected.</h2>
        <p className="max-w-xs text-sm leading-relaxed text-[#a8b3c3]">Orders, products and deliveries.<br />The right information in the right place.</p>
      </div>
      <div className="grid grid-flow-dense grid-cols-1 gap-px overflow-hidden rounded-3xl border border-white/15 bg-white/15 lg:grid-cols-3 lg:grid-rows-2">
        <article className="relative flex min-h-96 flex-col justify-between overflow-hidden bg-[#172334] p-7 sm:p-10 lg:col-span-2 lg:row-span-2">
          <div aria-hidden="true" className="absolute -right-16 top-16 size-72 rounded-full border border-[#d5f56b]/15 sm:size-96"><div className="absolute inset-10 rounded-full border border-[#d5f56b]/20" /><div className="absolute inset-20 rounded-full border border-[#d5f56b]/25" /></div>
          <div className="relative"><Package size={32} weight="light" className="text-[#d5f56b]" aria-hidden="true" /><h3 className="mt-10 max-w-md text-3xl font-medium leading-tight tracking-tight sm:text-4xl">Every step is clear.<br />Every order has a path.</h3><p className="mt-5 max-w-sm text-sm leading-relaxed text-[#b9c4d4]">Track statuses, view details and assign deliveries within your operations workflow.</p></div>
          <div className="relative mt-16 flex flex-wrap items-center gap-3 border-t border-white/15 pt-7 text-xs text-[#c6ceda]" aria-label="Workflow steps"><span>Order</span><span aria-hidden="true" className="text-[#d5f56b]">→</span><span>Process</span><span aria-hidden="true" className="text-[#d5f56b]">→</span><span>Deliver</span></div>
        </article>
        <article className="bg-[#101925] p-7 sm:p-9"><Cube size={28} weight="light" className="text-[#d5f56b]" aria-hidden="true" /><h3 className="mt-7 text-xl font-medium tracking-tight">Know your stock.</h3><p className="mt-3 text-sm leading-relaxed text-[#a8b3c3]">Manage products, variants, prices and actual stock levels.</p></article>
        <article className="bg-[#d5f56b] p-7 text-[#16200d] sm:p-9"><Truck size={28} weight="light" aria-hidden="true" /><h3 className="mt-7 text-xl font-medium tracking-tight">Keep the journey moving.</h3><p className="mt-3 text-sm leading-relaxed text-[#344622]">Clearly assigned orders. Delivery outcomes updated in one place.</p></article>
      </div>
    </section>

    <section aria-labelledby="roles-title" className="mx-auto max-w-6xl px-5 pb-28 sm:px-8 md:pb-40">
      <h2 id="roles-title" className="mb-10 text-[clamp(2rem,4.5vw,3.75rem)] font-medium leading-tight tracking-[-0.05em]">The right role. The right workspace.</h2>
      <div data-accordion className="flex flex-col gap-px overflow-hidden rounded-3xl border border-white/15 bg-white/15 md:min-h-80 md:flex-row">
        {roles.map((role, index) => {
          const Icon = role.icon
          const open = activeRole === index
          return <article key={role.title} className={`min-w-0 bg-[#111b29] transition-[flex-grow,background-color] duration-500 motion-reduce:transition-none md:basis-0 ${open ? 'md:grow-[2] bg-[#1b293a]' : 'md:grow'}`}>
            <h3><button type="button" id={`${id}-role-${index}`} aria-expanded={open} aria-controls={`${id}-panel-${index}`} onClick={() => setActiveRole(index)} onKeyDown={event => navigateRoles(event, index)} className={`group flex w-full items-center justify-between gap-3 p-7 text-left text-lg font-medium text-[#f0f2ed] hover:text-[#d5f56b] focus-visible:outline-offset-[-5px] sm:p-9 ${focus}`}><span>{role.title}</span><ArrowUpRight size={22} aria-hidden="true" className={`shrink-0 transition-transform motion-reduce:transition-none ${open ? 'rotate-45 text-[#d5f56b]' : 'group-hover:-translate-y-1'}`} /></button></h3>
            <div id={`${id}-panel-${index}`} aria-labelledby={`${id}-role-${index}`} role="region" hidden={!open} className="px-7 pb-9 sm:px-9"><Icon size={44} weight="light" aria-hidden="true" className="mb-6 text-[#d5f56b]" /><p className="max-w-xs text-sm leading-relaxed text-[#c6ceda]">{role.description}</p></div>
          </article>
        })}
      </div>
    </section>

    <section data-desire aria-labelledby="desire-title" className="bg-[#101925] px-5 py-28 sm:px-8 md:py-40">
      <div className="mx-auto max-w-6xl">
        <h2 id="desire-title" className="max-w-4xl text-[clamp(2rem,4.8vw,4.25rem)] font-medium leading-[1.25] tracking-[-0.045em]">Make room for <span className="mx-1 inline-block h-[0.7em] w-[1.7em] overflow-hidden rounded-full bg-[#2d4054] align-middle"><img src="https://picsum.photos/seed/quiet-workspace/240/100" alt="Illustrative landscape representing calm" width="240" height="100" loading="lazy" onError={hideFailedImage} className="size-full object-cover grayscale" /></span> focus.</h2>
        <p className="mt-12 max-w-4xl text-[clamp(1.5rem,3.4vw,3rem)] font-medium leading-[1.4] tracking-tight text-[#d5f56b]">{statement.split(' ').map((word, index) => <span key={`${word}-${index}`} data-word className="inline-block">{word}<span aria-hidden="true">&nbsp;</span></span>)}</p>
        <figure data-desire-media className="relative mt-16 overflow-hidden rounded-3xl bg-[#223247]">
          <div data-desire-image className="aspect-[4/3] sm:aspect-[16/7]"><img src="https://picsum.photos/seed/open-road-journey/1600/800" alt="Illustrative landscape representing a journey, not an actual delivery photograph" width="1600" height="800" loading="lazy" onError={hideFailedImage} className="size-full object-cover grayscale" /></div>
          <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#0b111b]/70 to-transparent" />
          <figcaption className="absolute bottom-6 left-6 right-6 text-xs text-[#e0e6ed] sm:bottom-8 sm:left-9">A wider perspective. A clearer workflow. <span className="mt-1 block text-[#b9c4d4]">Illustrative image.</span></figcaption>
        </figure>
      </div>
    </section>

    <section aria-label="Products, orders, deliveries — one flow" className="relative overflow-hidden border-y border-white/15 bg-[#0b111b] py-10 sm:py-14">
      <div aria-hidden="true" data-marquee-track className="flex w-max text-[clamp(2.5rem,7vw,6rem)] font-medium leading-none tracking-[-0.04em] text-[#64748b]">
        {[0, 1].map(copy => <div key={copy} className="flex shrink-0 items-center gap-10 pr-10"><span>PRODUCTS</span><ArrowUpRight className="size-10 text-[#d5f56b] sm:size-16" /><span>ORDERS</span><ArrowUpRight className="size-10 text-[#d5f56b] sm:size-16" /><span>DELIVERIES</span><ArrowUpRight className="size-10 text-[#d5f56b] sm:size-16" /></div>)}
      </div>
      <button type="button" onClick={toggleMarquee} aria-label={paused ? 'Resume moving text' : 'Pause moving text'} aria-pressed={paused} className={`absolute bottom-2 right-3 flex size-9 items-center justify-center rounded-full border border-white/25 bg-[#111b29] text-white motion-reduce:hidden ${focus}`}>{paused ? <Play size={15} aria-hidden="true" /> : <Pause size={15} aria-hidden="true" />}</button>
    </section>

    <section id="signin" aria-labelledby="signin-title" className="mx-auto grid max-w-6xl scroll-mt-8 items-center gap-14 px-5 py-28 sm:px-8 md:py-40 lg:grid-cols-2 lg:gap-24">
      <div><p className="mb-6 text-sm text-[#d5f56b]">Your workspace is ready.</p><h2 id="signin-title" className="text-[clamp(2.5rem,5vw,4.5rem)] font-medium leading-[1.1] tracking-[-0.055em]">Start a<br />clearer day.</h2><p className="mt-7 max-w-sm text-sm leading-relaxed text-[#a8b3c3]">Sign in with your system account. Access is determined by your role.</p><div className="mt-10 flex items-center gap-3 text-xs text-[#a8b3c3]"><span aria-hidden="true" className="h-px w-10 bg-[#d5f56b]/50" />Order Workspace</div></div>
      <div className="welcome-signin min-w-0 rounded-3xl border border-white/15 bg-[#111b29] p-7 text-[#f0f2ed] shadow-[0_24px_80px_#00000030] sm:p-10">{children}</div>
    </section>
    <footer className="mx-auto flex max-w-6xl flex-col justify-between gap-5 border-t border-white/15 px-5 py-8 text-xs text-[#a8b3c3] sm:flex-row sm:px-8"><span className="text-[#f0f2ed]">order / workspace</span><span>Products. Operations. Deliveries.</span><a href="#welcome" className={`flex items-center gap-2 hover:text-[#d5f56b] ${focus}`}>Back to top<ArrowUpRight size={15} aria-hidden="true" /></a></footer>
  </main>
}
