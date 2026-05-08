<template>
  <div class="page">
    <header class="top-bar">
      <div class="top-bar-inner">
        <router-link to="/" class="brand">
          <span class="logo">&#9650;</span>
          <span>IoTeca</span>
        </router-link>
        <nav class="top-nav">
          <router-link to="/login" class="btn-ghost">Log in</router-link>
          <router-link to="/register" class="btn-primary">Get started</router-link>
        </nav>
      </div>
    </header>

    <div class="content">
      <div class="inner">
        <h1>About IoTeca</h1>
        <p class="lead">
          IoTeca is a cloud backend built for IoT projects. It handles device authentication, metric ingestion,
          alerting, and notifications — so your team can focus on the application layer, not the infrastructure.
        </p>

        <section>
          <h2>How to set up your IoT cloud</h2>
          <ol class="steps">
            <li>
              <strong>Register an account</strong>
              <p>Go to <router-link to="/register">Get started</router-link> and create your account with an email address and password.</p>
            </li>
            <li>
              <strong>Create a project</strong>
              <p>
                A project is a logical namespace for a set of devices. From the dashboard, click
                <em>New Project</em> and give it a name. Each project gets its own ingestion endpoint.
              </p>
            </li>
            <li>
              <strong>Generate an API key</strong>
              <p>
                Open the project and create an API key. Set an optional expiry date. Your devices will
                include this key in the <code>X-API-Key</code> header on every request.
              </p>
            </li>
            <li>
              <strong>Push metric readings</strong>
              <p>
                From any device or server, send an HTTP POST to the ingestion endpoint:
              </p>
              <pre><code>POST /api/ingest
X-API-Key: &lt;your-key&gt;
Content-Type: application/json

{
  "metric": "temperature",
  "value": 24.5,
  "unit": "celsius"
}</code></pre>
            </li>
            <li>
              <strong>Configure alert rules</strong>
              <p>
                In the dashboard, define threshold rules for any metric — for example,
                trigger an alert when <code>temperature &gt; 50</code>. Choose email or webhook as the delivery channel.
              </p>
            </li>
            <li>
              <strong>Monitor your quota</strong>
              <p>
                Your account has a daily ingestion quota. The dashboard shows live usage.
                You will receive an email warning at 80% usage and again when the limit is reached.
              </p>
            </li>
          </ol>
        </section>

        <section>
          <h2>How your data is stored and handled</h2>
          <ul class="list">
            <li>
              <strong>Storage</strong> — Metric readings are stored in a PostgreSQL database hosted in a private
              cloud environment. Data is logically partitioned per project; no project can access another's data.
            </li>
            <li>
              <strong>Retention</strong> — Data is retained for as long as your account is active. You can delete
              individual projects (and all their data) from the dashboard at any time. Account deletion removes
              all associated data permanently within 30 days. See our
              <router-link to="/privacy">Privacy &amp; Deletion Policy</router-link> for details.
            </li>
            <li>
              <strong>Encryption</strong> — All data in transit is encrypted with TLS. API keys are stored as
              hashed values; plaintext keys are shown only once at creation time.
            </li>
            <li>
              <strong>Access control</strong> — Only authenticated users with the correct role can access a
              project's data. Device-level API keys are scoped to ingestion only; they cannot read or modify
              project settings.
            </li>
            <li>
              <strong>No data selling</strong> — IoTeca does not sell, share, or process your device data for
              any purpose other than delivering the service to you.
            </li>
          </ul>
        </section>

        <div class="cta-row">
          <router-link to="/register" class="cta-primary">Create an account</router-link>
          <router-link to="/" class="cta-ghost">Back to home</router-link>
        </div>
      </div>
    </div>

    <footer class="footer">
      <div class="footer-inner">
        <nav class="footer-links">
          <router-link to="/">Home</router-link>
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
  name: 'AboutView',
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #e0e0e0;
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
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.2s;
}
.btn-ghost {
  padding: 7px 18px;
  border: 1.5px solid rgba(255,255,255,0.25);
  border-radius: 7px;
  color: #e0e0e0;
}
.btn-ghost:hover { border-color: #4fc3f7; color: #4fc3f7; }
.btn-primary {
  padding: 7px 18px;
  background: #1a73e8;
  border-radius: 7px;
  color: #fff;
  font-weight: 500;
}
.btn-primary:hover { background: #1558b0; }

/* ── Content ── */
.content {
  padding: 64px 24px 80px;
}
.inner {
  max-width: 740px;
  margin: 0 auto;
}
h1 {
  font-size: clamp(1.75rem, 4vw, 2.5rem);
  color: #fff;
  margin-bottom: 16px;
}
.lead {
  font-size: 1.05rem;
  color: #b0bec5;
  line-height: 1.75;
  margin-bottom: 48px;
}
section { margin-bottom: 56px; }
h2 {
  font-size: 1.35rem;
  color: #4fc3f7;
  margin-bottom: 24px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(79, 195, 247, 0.2);
}

/* ── Steps ── */
.steps {
  list-style: none;
  padding: 0;
  counter-reset: steps;
  display: flex;
  flex-direction: column;
  gap: 28px;
}
.steps li {
  counter-increment: steps;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 44px;
  position: relative;
}
.steps li::before {
  content: counter(steps);
  position: absolute;
  left: 0;
  top: 0;
  width: 28px;
  height: 28px;
  background: #1a73e8;
  border-radius: 50%;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.steps li strong { color: #e0e0e0; font-size: 1rem; }
.steps li p { color: #90a4ae; font-size: 0.9rem; line-height: 1.65; margin: 0; }
.steps li a { color: #4fc3f7; text-decoration: none; }
.steps li a:hover { text-decoration: underline; }

pre {
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin-top: 10px;
}
pre code {
  font-family: monospace;
  font-size: 0.85rem;
  color: #80cbc4;
  background: none;
  padding: 0;
}
code {
  background: rgba(255,255,255,0.08);
  padding: 2px 7px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.88em;
  color: #4fc3f7;
}

/* ── List ── */
.list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.list li {
  padding-left: 20px;
  border-left: 3px solid rgba(79,195,247,0.35);
  color: #90a4ae;
  font-size: 0.9rem;
  line-height: 1.65;
}
.list li strong { color: #e0e0e0; display: block; margin-bottom: 4px; }
.list li a { color: #4fc3f7; text-decoration: none; }
.list li a:hover { text-decoration: underline; }

/* ── CTA row ── */
.cta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.cta-primary {
  padding: 13px 28px;
  background: #1a73e8;
  color: #fff;
  border-radius: 8px;
  font-size: 0.975rem;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.2s;
}
.cta-primary:hover { background: #1558b0; }
.cta-ghost {
  padding: 13px 28px;
  border: 1.5px solid rgba(255,255,255,0.25);
  color: #e0e0e0;
  border-radius: 8px;
  font-size: 0.975rem;
  text-decoration: none;
  transition: border-color 0.2s;
}
.cta-ghost:hover { border-color: #4fc3f7; color: #4fc3f7; }

/* ── Footer ── */
.footer {
  border-top: 1px solid rgba(255,255,255,0.07);
  padding: 32px 24px;
}
.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
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
.footer-copy { font-size: 0.8rem; color: #546e7a; }
</style>
