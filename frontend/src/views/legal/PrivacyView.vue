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
        <p class="doc-label">Legal</p>
        <h1>Privacy &amp; Data Deletion Policy</h1>
        <p class="effective">Effective date: 1 May 2026</p>

        <p class="lead">
          IoTeca is committed to handling your data responsibly. This policy explains what data we collect,
          how we use it, how long we keep it, and how you can request deletion.
        </p>

        <section>
          <h2>1. Data we collect</h2>
          <ul>
            <li><strong>Account data</strong> — email address, hashed password, account role, and registration date.</li>
            <li><strong>Project data</strong> — project names, descriptions, and configuration you create.</li>
            <li><strong>API keys</strong> — key metadata (name, expiry, creation date). The plaintext key value is shown once and is never stored.</li>
            <li><strong>Device metrics</strong> — all readings your devices push to the ingestion endpoint, including metric name, value, unit, and timestamp.</li>
            <li><strong>Alert rules and logs</strong> — the threshold rules you define and the alert events they produce.</li>
            <li><strong>Notification logs</strong> — records of emails and webhooks sent, including delivery status.</li>
            <li><strong>Usage data</strong> — daily ingestion counts used to enforce quotas. We do not collect browser telemetry, cookies, or tracking data.</li>
          </ul>
        </section>

        <section>
          <h2>2. How we use your data</h2>
          <p>All data collected is used exclusively to provide the Service:</p>
          <ul>
            <li>Authenticating users and devices.</li>
            <li>Storing and retrieving metric readings on your behalf.</li>
            <li>Evaluating alert rules and delivering notifications.</li>
            <li>Enforcing quotas and displaying usage statistics.</li>
          </ul>
          <p>
            IoTeca does not sell your data, use it for advertising, or share it with third parties
            except as required to deliver the Service (e.g., sending email via a mail provider).
          </p>
        </section>

        <section>
          <h2>3. Data retention</h2>
          <ul>
            <li><strong>Active accounts</strong> — data is retained for the lifetime of your account.</li>
            <li><strong>Deleted projects</strong> — when you delete a project from the dashboard, all associated metrics, alert rules, API keys, and logs are permanently deleted within 24 hours.</li>
            <li><strong>Closed accounts</strong> — when you delete your account, all of your data (account record, projects, metrics, keys, logs) is permanently and irreversibly deleted within 30 days.</li>
            <li><strong>Notification logs</strong> — retained for 90 days to support delivery troubleshooting, then automatically purged.</li>
          </ul>
        </section>

        <section>
          <h2>4. How to delete your data</h2>

          <h3>Delete a project</h3>
          <p>
            Open the project in the dashboard and use the <em>Delete project</em> option in project settings.
            This immediately queues deletion of all metrics, alert rules, API keys, and logs associated with
            that project. Deletion completes within 24 hours.
          </p>

          <h3>Delete your account</h3>
          <p>
            Go to account settings and select <em>Delete account</em>. You will be asked to confirm with
            your password. Once confirmed:
          </p>
          <ul>
            <li>Your session will be invalidated immediately.</li>
            <li>All projects and their data will be queued for deletion.</li>
            <li>All data will be permanently deleted within 30 days with no possibility of recovery.</li>
          </ul>

          <h3>Request deletion by email</h3>
          <p>
            If you cannot access your account, send a deletion request to
            <a href="mailto:privacy@ioteca.io">privacy@ioteca.io</a> from the email address registered to
            the account. We will process the request within 7 business days and confirm by email when deletion is complete.
          </p>
        </section>

        <section>
          <h2>5. Data security</h2>
          <ul>
            <li>All data in transit is encrypted with TLS 1.2 or higher.</li>
            <li>Passwords are stored as salted hashes using bcrypt. Plaintext passwords are never stored.</li>
            <li>API keys are stored as hashed values. The plaintext key is shown only once at creation.</li>
            <li>Access to production databases is restricted to authorised personnel and automated services only.</li>
          </ul>
        </section>

        <section>
          <h2>6. Your rights</h2>
          <p>You have the right to:</p>
          <ul>
            <li><strong>Access</strong> — request a copy of the personal data we hold about you.</li>
            <li><strong>Correction</strong> — update inaccurate account information from your account settings.</li>
            <li><strong>Deletion</strong> — request permanent deletion of your data as described in Section 4.</li>
            <li><strong>Portability</strong> — request an export of your metric data in JSON format by contacting support.</li>
          </ul>
          <p>
            To exercise any of these rights, contact <a href="mailto:privacy@ioteca.io">privacy@ioteca.io</a>.
          </p>
        </section>

        <section>
          <h2>7. Cookies</h2>
          <p>
            IoTeca uses only functional session storage in the browser (JWT token) required for authentication.
            We do not use analytics cookies, tracking pixels, or third-party advertising trackers.
          </p>
        </section>

        <section>
          <h2>8. Changes to this policy</h2>
          <p>
            We may update this policy when the Service changes in ways that affect data handling.
            We will notify registered users by email at least 14 days before material changes take effect.
            The effective date at the top of this page will always reflect the current version.
          </p>
        </section>

        <section>
          <h2>9. Contact</h2>
          <p>
            For privacy-related questions or data deletion requests, email
            <a href="mailto:privacy@ioteca.io">privacy@ioteca.io</a>. For general support, contact
            <a href="mailto:support@ioteca.io">support@ioteca.io</a>.
          </p>
        </section>

        <div class="nav-row">
          <router-link to="/terms" class="link-card">&#8592; Terms &amp; Conditions</router-link>
          <router-link to="/" class="cta-ghost">Back to home</router-link>
        </div>
      </div>
    </div>

    <footer class="footer">
      <div class="footer-inner">
        <nav class="footer-links">
          <router-link to="/">Home</router-link>
          <router-link to="/about">About</router-link>
          <router-link to="/terms">Terms &amp; Conditions</router-link>
        </nav>
        <p class="footer-copy">&copy; 2026 IoTeca. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'PrivacyView',
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
.top-nav { display: flex; align-items: center; gap: 16px; }
.top-nav a { text-decoration: none; font-size: 0.9rem; }
.btn-ghost {
  padding: 7px 18px;
  border: 1.5px solid rgba(255,255,255,0.25);
  border-radius: 7px;
  color: #e0e0e0;
  transition: border-color 0.2s;
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
.content { padding: 64px 24px 80px; }
.inner { max-width: 740px; margin: 0 auto; }

.doc-label {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #4fc3f7;
  margin-bottom: 8px;
}
h1 {
  font-size: clamp(1.75rem, 4vw, 2.4rem);
  color: #fff;
  margin-bottom: 8px;
}
.effective { font-size: 0.85rem; color: #546e7a; margin-bottom: 28px; }
.lead {
  font-size: 1rem;
  color: #b0bec5;
  line-height: 1.75;
  margin-bottom: 48px;
  padding: 16px 20px;
  background: rgba(255,255,255,0.04);
  border-left: 3px solid #1a73e8;
  border-radius: 0 8px 8px 0;
}

section { margin-bottom: 40px; }
h2 {
  font-size: 1.05rem;
  color: #fff;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
h3 {
  font-size: 0.95rem;
  color: #b0bec5;
  margin: 18px 0 8px;
}
p {
  color: #90a4ae;
  font-size: 0.9rem;
  line-height: 1.75;
  margin-bottom: 10px;
}
ul {
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}
ul li {
  color: #90a4ae;
  font-size: 0.9rem;
  line-height: 1.65;
}
ul li strong { color: #b0bec5; }
a { color: #4fc3f7; text-decoration: none; }
a:hover { text-decoration: underline; }

/* ── Nav row ── */
.nav-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid rgba(255,255,255,0.08);
}
.link-card {
  padding: 13px 24px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  color: #e0e0e0;
  font-size: 0.9rem;
  text-decoration: none;
  transition: border-color 0.2s;
}
.link-card:hover { border-color: #4fc3f7; color: #4fc3f7; text-decoration: none; }
.cta-ghost {
  padding: 13px 24px;
  border: 1.5px solid rgba(255,255,255,0.2);
  color: #90a4ae;
  border-radius: 8px;
  font-size: 0.9rem;
  text-decoration: none;
}
.cta-ghost:hover { border-color: #4fc3f7; color: #4fc3f7; text-decoration: none; }

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
}
.footer-links a:hover { color: #fff; text-decoration: none; }
.footer-copy { font-size: 0.8rem; color: #546e7a; }
</style>
