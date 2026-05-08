<template>
  <div class="landing">
    <header class="top-bar">
      <div class="top-bar-inner">
        <div class="brand">
          <span class="logo">&#9650;</span>
          <span>IoTeca</span>
        </div>
        <nav class="top-nav">
          <router-link to="/about">About</router-link>
          <router-link to="/login" class="btn-ghost">Log in</router-link>
          <router-link to="/register" class="btn-primary">Get started</router-link>
        </nav>
      </div>
    </header>

    <section class="hero">
      <div class="hero-inner">
        <p class="hero-eyebrow">IoT Cloud Platform</p>
        <h1>Connect, monitor, and act on your IoT data — in real time</h1>
        <p class="hero-sub">
          IoTeca gives you a secure cloud backend for your devices: ingest sensor readings,
          set alert rules, receive notifications, and manage access — all through a simple REST API.
        </p>
        <div class="hero-ctas">
          <router-link to="/register" class="cta-primary">Create free account</router-link>
          <router-link to="/about" class="cta-ghost">Learn how it works</router-link>
        </div>
      </div>
    </section>

    <section class="about-strip">
      <div class="strip-inner">
        <h2>What is IoTeca?</h2>
        <p>
          IoTeca is a self-service cloud platform designed for developers and teams building connected devices.
          You register your projects, issue API keys to your devices, and the platform handles
          time-series ingestion, threshold alerting, email and webhook notifications, and usage quotas —
          so you can focus on your application, not the infrastructure.
        </p>
      </div>
    </section>

    <section class="services">
      <div class="services-inner">
        <h2>What's included</h2>
        <div class="cards">
          <div class="card">
            <div class="card-icon">&#128268;</div>
            <h3>Device ingestion</h3>
            <p>Push metric readings from any device over HTTPS using project API keys. No SDK required.</p>
          </div>
          <div class="card">
            <div class="card-icon">&#128276;</div>
            <h3>Alert engine</h3>
            <p>Define threshold rules per metric. The platform evaluates every reading and fires alerts automatically.</p>
          </div>
          <div class="card">
            <div class="card-icon">&#9993;</div>
            <h3>Notifications</h3>
            <p>Receive alert events by email or webhook. Configure per-project delivery addresses.</p>
          </div>
          <div class="card">
            <div class="card-icon">&#128202;</div>
            <h3>Quota management</h3>
            <p>Each account has a daily ingestion quota. Monitor usage and get notified before you hit your limit.</p>
          </div>
        </div>
      </div>
    </section>

    <section class="access">
      <div class="access-inner">
        <h2>How to access the platform</h2>
        <ol class="steps">
          <li><strong>Create an account</strong> — register with your email address.</li>
          <li><strong>Create a project</strong> — each project groups your devices and its data.</li>
          <li><strong>Issue an API key</strong> — your devices use this key to authenticate ingestion requests.</li>
          <li><strong>Push data</strong> — send <code>POST /ingest</code> with your metric readings.</li>
          <li><strong>Set alert rules</strong> — define thresholds in the dashboard and choose notification channels.</li>
        </ol>
        <div class="access-ctas">
          <router-link to="/register" class="cta-primary">Get started for free</router-link>
          <router-link to="/login" class="cta-ghost">Log in</router-link>
        </div>
      </div>
    </section>

    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="logo">&#9650;</span>
          <span>IoTeca</span>
        </div>
        <nav class="footer-links">
          <router-link to="/about">About</router-link>
          <router-link to="/terms">Terms &amp; Conditions</router-link>
          <router-link to="/privacy">Privacy &amp; Deletion</router-link>
        </nav>
        <p class="footer-copy">&copy; 2026 IoTeca. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'LandingView',
}
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #e0e0e0;
  font-family: inherit;
}

/* ── Top bar ── */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(26, 26, 46, 0.85);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.top-bar-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4fc3f7;
  font-weight: 700;
  font-size: 1.15rem;
  text-decoration: none;
}
.brand .logo { font-size: 1.3rem; }
.top-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}
.top-nav a {
  color: #ccc;
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.2s;
}
.top-nav a:hover { color: #fff; }
.btn-ghost {
  padding: 7px 18px;
  border: 1.5px solid rgba(255,255,255,0.25);
  border-radius: 7px;
  color: #e0e0e0 !important;
}
.btn-ghost:hover { border-color: #4fc3f7; color: #4fc3f7 !important; }
.btn-primary {
  padding: 7px 18px;
  background: #1a73e8;
  border-radius: 7px;
  color: #fff !important;
  font-weight: 500;
}
.btn-primary:hover { background: #1558b0; }

/* ── Hero ── */
.hero {
  padding: 100px 24px 80px;
}
.hero-inner {
  max-width: 760px;
  margin: 0 auto;
  text-align: center;
}
.hero-eyebrow {
  font-size: 0.85rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #4fc3f7;
  margin-bottom: 16px;
}
.hero h1 {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  margin-bottom: 20px;
}
.hero-sub {
  font-size: 1.1rem;
  color: #b0bec5;
  line-height: 1.7;
  max-width: 620px;
  margin: 0 auto 36px;
}
.hero-ctas {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}
.cta-primary {
  padding: 14px 32px;
  background: #1a73e8;
  color: #fff;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.2s;
}
.cta-primary:hover { background: #1558b0; }
.cta-ghost {
  padding: 14px 32px;
  border: 1.5px solid rgba(255,255,255,0.3);
  color: #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  text-decoration: none;
  transition: border-color 0.2s, color 0.2s;
}
.cta-ghost:hover { border-color: #4fc3f7; color: #4fc3f7; }

/* ── About strip ── */
.about-strip {
  background: rgba(255,255,255,0.04);
  padding: 64px 24px;
  border-top: 1px solid rgba(255,255,255,0.07);
  border-bottom: 1px solid rgba(255,255,255,0.07);
}
.strip-inner {
  max-width: 720px;
  margin: 0 auto;
  text-align: center;
}
.strip-inner h2 {
  font-size: 1.6rem;
  color: #fff;
  margin-bottom: 16px;
}
.strip-inner p {
  color: #b0bec5;
  font-size: 1rem;
  line-height: 1.75;
}

/* ── Services ── */
.services {
  padding: 72px 24px;
}
.services-inner {
  max-width: 1100px;
  margin: 0 auto;
}
.services-inner h2 {
  text-align: center;
  font-size: 1.6rem;
  color: #fff;
  margin-bottom: 40px;
}
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
}
.card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 14px;
  padding: 32px 24px;
  transition: border-color 0.2s;
}
.card:hover { border-color: #4fc3f7; }
.card-icon { font-size: 2rem; margin-bottom: 16px; }
.card h3 { font-size: 1.05rem; color: #fff; margin-bottom: 10px; }
.card p { font-size: 0.9rem; color: #90a4ae; line-height: 1.65; }

/* ── Access section ── */
.access {
  background: rgba(255,255,255,0.04);
  padding: 72px 24px;
  border-top: 1px solid rgba(255,255,255,0.07);
}
.access-inner {
  max-width: 680px;
  margin: 0 auto;
}
.access-inner h2 {
  font-size: 1.6rem;
  color: #fff;
  margin-bottom: 28px;
  text-align: center;
}
.steps {
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  color: #b0bec5;
  font-size: 0.975rem;
  line-height: 1.65;
  margin-bottom: 36px;
}
.steps strong { color: #e0e0e0; }
.steps code {
  background: rgba(255,255,255,0.1);
  padding: 2px 7px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.9em;
  color: #4fc3f7;
}
.access-ctas {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

/* ── Footer ── */
.footer {
  border-top: 1px solid rgba(255,255,255,0.07);
  padding: 40px 24px;
}
.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4fc3f7;
  font-weight: 700;
  font-size: 1rem;
}
.footer-links {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  justify-content: center;
}
.footer-links a {
  color: #90a4ae;
  text-decoration: none;
  font-size: 0.875rem;
  transition: color 0.2s;
}
.footer-links a:hover { color: #fff; }
.footer-copy {
  font-size: 0.8rem;
  color: #546e7a;
}
</style>
