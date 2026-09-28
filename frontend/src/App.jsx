import React, { useState, useEffect } from 'react';
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [notification, setNotification] = useState(null);

  // Data states
  const [summary, setSummary] = useState(null);
  const [flaggedByRule, setFlaggedByRule] = useState({});
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [pendingReviews, setPendingReviews] = useState([]);
  const [rules, setRules] = useState([]);

  // Form states
  const [newAccount, setNewAccount] = useState({
    accountNumber: '',
    ownerName: '',
    balance: '',
    currency: 'INR'
  });

  const [newTx, setNewTx] = useState({
    senderAccountId: '',
    receiverAccountId: '',
    amount: ''
  });

  const [txResult, setTxResult] = useState(null);

  const [newRule, setNewRule] = useState({
    name: '',
    code: '',
    description: '',
    enabled: true,
    amountThreshold: '',
    transactionCountThreshold: '',
    windowSeconds: ''
  });

  const [editingRule, setEditingRule] = useState(null);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 5000);
  };

  const loadDashboard = async () => {
    try {
      const [sumRes, ruleRes] = await Promise.all([
        api.get('/dashboard/summary'),
        api.get('/dashboard/flagged-by-rule')
      ]);
      setSummary(sumRes.data);
      setFlaggedByRule(ruleRes.data);
    } catch (err) {
      console.error('Failed to load dashboard:', err);
    }
  };

  const loadAccounts = async () => {
    try {
      const res = await api.get('/accounts');
      setAccounts(res.data);
    } catch (err) {
      console.error('Failed to load accounts:', err);
    }
  };

  const loadTransactions = async () => {
    try {
      const res = await api.get('/transactions');
      setTransactions(res.data);
    } catch (err) {
      console.error('Failed to load transactions:', err);
    }
  };

  const loadReviews = async () => {
    try {
      const res = await api.get('/reviews/pending');
      setPendingReviews(res.data);
    } catch (err) {
      console.error('Failed to load pending reviews:', err);
    }
  };

  const loadRules = async () => {
    try {
      const res = await api.get('/rules');
      setRules(res.data);
    } catch (err) {
      console.error('Failed to load rules:', err);
    }
  };

  const refreshAll = () => {
    loadDashboard();
    loadAccounts();
    loadTransactions();
    loadReviews();
    loadRules();
  };

  useEffect(() => {
    refreshAll();
  }, []);

  useEffect(() => {
    if (activeTab === 'dashboard') loadDashboard();
    if (activeTab === 'accounts') loadAccounts();
    if (activeTab === 'transactions') {
      loadTransactions();
      loadAccounts();
    }
    if (activeTab === 'review') loadReviews();
    if (activeTab === 'rules') loadRules();
  }, [activeTab]);

  // Account creation
  const handleCreateAccount = async (e) => {
    e.preventDefault();
    try {
      await api.post('/accounts', {
        ...newAccount,
        balance: parseFloat(newAccount.balance)
      });
      showNotification(`Account ${newAccount.accountNumber} created successfully!`);
      setNewAccount({ accountNumber: '', ownerName: '', balance: '', currency: 'INR' });
      loadAccounts();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to create account';
      showNotification(msg, 'error');
    }
  };

  // Transaction creation
  const handleCreateTransaction = async (e) => {
    e.preventDefault();
    setTxResult(null);
    try {
      const res = await api.post('/transactions', {
        senderAccountId: parseInt(newTx.senderAccountId),
        receiverAccountId: parseInt(newTx.receiverAccountId),
        amount: parseFloat(newTx.amount)
      });
      setTxResult(res.data);
      if (res.data.status === 'FLAGGED') {
        showNotification(`⚠️ Transaction FLAGGED for manual review!`, 'error');
      } else {
        showNotification(`✓ Transaction COMPLETED successfully!`, 'success');
      }
      setNewTx({ senderAccountId: '', receiverAccountId: '', amount: '' });
      loadTransactions();
      loadAccounts();
      loadDashboard();
      loadReviews();
    } catch (err) {
      const msg = err.response?.data?.message || 'Transaction submission failed';
      showNotification(msg, 'error');
    }
  };

  // Review Actions
  const handleReviewAction = async (transactionId, action) => {
    try {
      await api.post(`/reviews/${transactionId}/${action}`, {
        reviewer: 'Admin Reviewer',
        reason: action === 'approve' ? 'Transaction approved after review' : 'Transaction blocked due to fraud risk'
      });
      showNotification(`Transaction #${transactionId} ${action.toUpperCase()}D successfully!`);
      loadReviews();
      loadDashboard();
      loadTransactions();
      loadAccounts();
    } catch (err) {
      const msg = err.response?.data?.message || `Failed to ${action} transaction`;
      showNotification(msg, 'error');
    }
  };

  // Rule operations
  const handleCreateRule = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        name: newRule.name,
        code: newRule.code,
        description: newRule.description,
        enabled: newRule.enabled,
        amountThreshold: newRule.amountThreshold ? parseFloat(newRule.amountThreshold) : null,
        transactionCountThreshold: newRule.transactionCountThreshold ? parseInt(newRule.transactionCountThreshold) : null,
        windowSeconds: newRule.windowSeconds ? parseInt(newRule.windowSeconds) : null
      };
      await api.post('/rules', payload);
      showNotification(`Rule "${newRule.name}" created successfully!`);
      setNewRule({
        name: '',
        code: '',
        description: '',
        enabled: true,
        amountThreshold: '',
        transactionCountThreshold: '',
        windowSeconds: ''
      });
      loadRules();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to create rule';
      showNotification(msg, 'error');
    }
  };

  const handleToggleRule = async (ruleId, currentEnabled) => {
    try {
      await api.patch(`/rules/${ruleId}/enabled`, { enabled: !currentEnabled });
      showNotification(`Rule status updated!`);
      loadRules();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to toggle rule';
      showNotification(msg, 'error');
    }
  };

  const handleUpdateRule = async (e) => {
    e.preventDefault();
    if (!editingRule) return;
    try {
      await api.put(`/rules/${editingRule.id}`, {
        ...editingRule,
        amountThreshold: editingRule.amountThreshold ? parseFloat(editingRule.amountThreshold) : null,
        transactionCountThreshold: editingRule.transactionCountThreshold ? parseInt(editingRule.transactionCountThreshold) : null,
        windowSeconds: editingRule.windowSeconds ? parseInt(editingRule.windowSeconds) : null
      });
      showNotification(`Rule updated successfully!`);
      setEditingRule(null);
      loadRules();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to update rule';
      showNotification(msg, 'error');
    }
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="brand-wrapper">
          <div className="brand-icon">🛡️</div>
          <div>
            <h1 className="brand-title">FraudWatch</h1>
            <p className="brand-subtitle">Digital Wallet Real-time Anomaly & Fraud Detection</p>
          </div>
        </div>
        <div className="status-badge">
          <span className="pulse-dot"></span>
          <span>Engine Active</span>
        </div>
      </header>

      {/* Navigation Tabs */}
      <nav className="nav-tabs">
        <button
          className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('dashboard')}
        >
          📊 Dashboard
        </button>
        <button
          className={`nav-tab ${activeTab === 'accounts' ? 'active' : ''}`}
          onClick={() => setActiveTab('accounts')}
        >
          💳 Accounts ({accounts.length})
        </button>
        <button
          className={`nav-tab ${activeTab === 'transactions' ? 'active' : ''}`}
          onClick={() => setActiveTab('transactions')}
        >
          💸 Transactions
        </button>
        <button
          className={`nav-tab ${activeTab === 'review' ? 'active' : ''}`}
          onClick={() => setActiveTab('review')}
        >
          🔍 Review Queue
          {pendingReviews.length > 0 && (
            <span className="tab-badge">{pendingReviews.length}</span>
          )}
        </button>
        <button
          className={`nav-tab ${activeTab === 'rules' ? 'active' : ''}`}
          onClick={() => setActiveTab('rules')}
        >
          ⚙️ Rules ({rules.length})
        </button>
      </nav>

      {/* Global Notifications */}
      {notification && (
        <div className={`alert alert-${notification.type}`}>
          <span>{notification.message}</span>
          <button className="alert-close" onClick={() => setNotification(null)}>×</button>
        </div>
      )}

      {/* TAB 1: DASHBOARD */}
      {activeTab === 'dashboard' && (
        <div>
          <div className="section-header">
            <h2 className="section-title">System Overview</h2>
            <button className="btn btn-secondary btn-sm" onClick={loadDashboard}>🔄 Refresh</button>
          </div>

          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-header">Total Transactions</div>
              <div className="stat-value">{summary?.totalTransactions ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-header">Completed</div>
              <div className="stat-value success">{summary?.completedTransactions ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-header">Flagged (Pending)</div>
              <div className="stat-value warning">{summary?.flaggedTransactions ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-header">Approved</div>
              <div className="stat-value purple">{summary?.approvedTransactions ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-header">Blocked</div>
              <div className="stat-value danger">{summary?.blockedTransactions ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-header">Pending Review</div>
              <div className="stat-value warning">{summary?.pendingReviews ?? 0}</div>
            </div>
          </div>

          <div className="section-header" style={{ marginTop: '28px' }}>
            <h2 className="section-title">Flagged Incidents By Rule</h2>
          </div>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-header">Amount Threshold (Over 50,000 INR)</div>
              <div className="stat-value danger">{flaggedByRule?.AMOUNT_THRESHOLD ?? 0}</div>
            </div>
            <div className="stat-card">
              <div className="stat-header">Velocity (&gt; 3 transfers in 60s)</div>
              <div className="stat-value warning">{flaggedByRule?.VELOCITY ?? 0}</div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: ACCOUNTS */}
      {activeTab === 'accounts' && (
        <div>
          <div className="form-card">
            <h3 style={{ marginBottom: '16px' }}>Create New Account</h3>
            <form onSubmit={handleCreateAccount} className="form-grid">
              <div className="form-group">
                <label>Account Number</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. ACC1005"
                  required
                  value={newAccount.accountNumber}
                  onChange={(e) => setNewAccount({ ...newAccount, accountNumber: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Owner Name</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Robert Brown"
                  required
                  value={newAccount.ownerName}
                  onChange={(e) => setNewAccount({ ...newAccount, ownerName: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Initial Balance (INR)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-input"
                  placeholder="e.g. 50000"
                  required
                  value={newAccount.balance}
                  onChange={(e) => setNewAccount({ ...newAccount, balance: e.target.value })}
                />
              </div>
              <div className="form-group">
                <button type="submit" className="btn btn-primary">
                  + Add Account
                </button>
              </div>
            </form>
          </div>

          <div className="section-header">
            <h2 className="section-title">Accounts Directory</h2>
            <button className="btn btn-secondary btn-sm" onClick={loadAccounts}>🔄 Refresh</button>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Account Number</th>
                  <th>Owner Name</th>
                  <th>Balance</th>
                  <th>Currency</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {accounts.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty-state">No accounts found</td>
                  </tr>
                ) : (
                  accounts.map((acc) => (
                    <tr key={acc.id}>
                      <td>#{acc.id}</td>
                      <td style={{ fontWeight: '600' }}>{acc.accountNumber}</td>
                      <td>{acc.ownerName}</td>
                      <td style={{ fontWeight: '600', color: '#10b981' }}>
                        ₹{Number(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                      <td>{acc.currency}</td>
                      <td>
                        <span className={`badge ${acc.active ? 'badge-completed' : 'badge-blocked'}`}>
                          {acc.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: TRANSACTIONS */}
      {activeTab === 'transactions' && (
        <div>
          <div className="form-card">
            <h3 style={{ marginBottom: '16px' }}>Send Transfer</h3>
            <form onSubmit={handleCreateTransaction} className="form-grid">
              <div className="form-group">
                <label>Sender Account</label>
                <select
                  className="form-select"
                  required
                  value={newTx.senderAccountId}
                  onChange={(e) => setNewTx({ ...newTx, senderAccountId: e.target.value })}
                >
                  <option value="">Select Sender</option>
                  {accounts.map((acc) => (
                    <option key={acc.id} value={acc.id}>
                      {acc.accountNumber} - {acc.ownerName} (Bal: ₹{Number(acc.balance).toFixed(2)})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Receiver Account</label>
                <select
                  className="form-select"
                  required
                  value={newTx.receiverAccountId}
                  onChange={(e) => setNewTx({ ...newTx, receiverAccountId: e.target.value })}
                >
                  <option value="">Select Receiver</option>
                  {accounts.map((acc) => (
                    <option key={acc.id} value={acc.id}>
                      {acc.accountNumber} - {acc.ownerName}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Transfer Amount (INR)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-input"
                  placeholder="e.g. 60000"
                  required
                  value={newTx.amount}
                  onChange={(e) => setNewTx({ ...newTx, amount: e.target.value })}
                />
              </div>

              <div className="form-group">
                <button type="submit" className="btn btn-primary">
                  🚀 Submit Transfer
                </button>
              </div>
            </form>

            {/* Immediate Result Card */}
            {txResult && (
              <div style={{ marginTop: '20px', padding: '16px', borderRadius: '8px',
                            border: `1px solid ${txResult.status === 'FLAGGED' ? '#f59e0b' : '#10b981'}`,
                            backgroundColor: txResult.status === 'FLAGGED' ? 'rgba(245, 158, 11, 0.1)' : 'rgba(16, 185, 129, 0.1)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong>Transaction #{txResult.id} ({txResult.transactionReference})</strong>
                  <span className={`badge badge-${txResult.status.toLowerCase()}`}>{txResult.status}</span>
                </div>
                {txResult.status === 'FLAGGED' ? (
                  <div style={{ marginTop: '10px' }}>
                    <p style={{ color: '#fbbf24', fontWeight: '600' }}>Suspicious transaction intercepted! Balances remain untouched.</p>
                    <ul style={{ marginTop: '6px', marginLeft: '20px', color: '#fca5a5' }}>
                      {txResult.flags.map((flag, idx) => (
                        <li key={idx}><strong>{flag.rule.name} ({flag.rule.code}):</strong> {flag.reason}</li>
                      ))}
                    </ul>
                  </div>
                ) : (
                  <p style={{ color: '#34d399', marginTop: '6px' }}>Transaction processed successfully. Sender and receiver balances updated.</p>
                )}
              </div>
            )}
          </div>

          <div className="section-header">
            <h2 className="section-title">Recent Transactions</h2>
            <button className="btn btn-secondary btn-sm" onClick={loadTransactions}>🔄 Refresh</button>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ref</th>
                  <th>Sender</th>
                  <th>Receiver</th>
                  <th>Amount</th>
                  <th>Timestamp</th>
                  <th>Status</th>
                  <th>Triggered Rules / Details</th>
                </tr>
              </thead>
              <tbody>
                {transactions.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-state">No transactions recorded yet</td>
                  </tr>
                ) : (
                  transactions.map((tx) => (
                    <tr key={tx.id}>
                      <td style={{ fontWeight: '600', color: '#60a5fa' }}>{tx.transactionReference}</td>
                      <td>{tx.senderAccountNumber} ({tx.senderOwnerName})</td>
                      <td>{tx.receiverAccountNumber} ({tx.receiverOwnerName})</td>
                      <td style={{ fontWeight: '600' }}>₹{Number(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                      <td style={{ fontSize: '12px', color: '#94a3b8' }}>
                        {tx.timestamp ? new Date(tx.timestamp).toLocaleString() : ''}
                      </td>
                      <td>
                        <span className={`badge badge-${tx.status.toLowerCase()}`}>
                          {tx.status}
                        </span>
                      </td>
                      <td>
                        {tx.flags && tx.flags.length > 0 ? (
                          <div style={{ fontSize: '12px' }}>
                            {tx.flags.map((f, i) => (
                              <div key={i} style={{ color: '#fbbf24', marginBottom: '2px' }}>
                                • <strong>{f.rule?.code}:</strong> {f.reason}
                              </div>
                            ))}
                          </div>
                        ) : (
                          <span style={{ color: '#64748b', fontSize: '12px' }}>None</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 4: REVIEW QUEUE */}
      {activeTab === 'review' && (
        <div>
          <div className="section-header">
            <h2 className="section-title">Flagged Transactions Review Queue</h2>
            <button className="btn btn-secondary btn-sm" onClick={loadReviews}>🔄 Refresh</button>
          </div>

          {pendingReviews.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">🎉</div>
              <h3>Review Queue is Empty!</h3>
              <p>No suspicious transactions require manual approval or blocking right now.</p>
            </div>
          ) : (
            <div className="review-grid">
              {pendingReviews.map((item) => (
                <div key={item.transaction.id} className="review-card">
                  <div className="review-card-header">
                    <span className="review-tx-ref">{item.transaction.transactionReference}</span>
                    <span className="badge badge-flagged">PENDING REVIEW</span>
                  </div>

                  <div className="review-card-body">
                    <div className="review-row">
                      <span className="label">Amount:</span>
                      <span className="val" style={{ color: '#ef4444', fontWeight: '700' }}>
                        ₹{Number(item.transaction.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </span>
                    </div>
                    <div className="review-row">
                      <span className="label">Sender:</span>
                      <span className="val">{item.transaction.sender?.accountNumber} ({item.transaction.sender?.ownerName})</span>
                    </div>
                    <div className="review-row">
                      <span className="label">Receiver:</span>
                      <span className="val">{item.transaction.receiver?.accountNumber} ({item.transaction.receiver?.ownerName})</span>
                    </div>
                    <div className="review-row">
                      <span className="label">Sender Available Balance:</span>
                      <span className="val">₹{Number(item.transaction.sender?.balance).toFixed(2)}</span>
                    </div>
                    <div className="review-row">
                      <span className="label">Time:</span>
                      <span className="val" style={{ fontSize: '12px', color: '#94a3b8' }}>
                        {new Date(item.transaction.timestamp).toLocaleString()}
                      </span>
                    </div>

                    <div className="flags-box">
                      <div className="flags-title">Triggered Anomaly Rules:</div>
                      {item.flags && item.flags.map((flag, idx) => (
                        <div key={idx} className="flag-item">
                          • <strong>{flag.rule?.code}:</strong> {flag.reason}
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="review-card-actions">
                    <button
                      className="btn btn-success"
                      onClick={() => handleReviewAction(item.transaction.id, 'approve')}
                    >
                      ✓ Approve Transfer
                    </button>
                    <button
                      className="btn btn-danger"
                      onClick={() => handleReviewAction(item.transaction.id, 'block')}
                    >
                      ✕ Block Transfer
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 5: RULES */}
      {activeTab === 'rules' && (
        <div>
          <div className="form-card">
            <h3 style={{ marginBottom: '16px' }}>Configure New Fraud Rule</h3>
            <form onSubmit={handleCreateRule} className="form-grid">
              <div className="form-group">
                <label>Rule Name</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. High Value Rule"
                  required
                  value={newRule.name}
                  onChange={(e) => setNewRule({ ...newRule, name: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Rule Code</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. AMOUNT_THRESHOLD or VELOCITY"
                  required
                  value={newRule.code}
                  onChange={(e) => setNewRule({ ...newRule, code: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Amount Threshold (INR)</label>
                <input
                  type="number"
                  step="0.01"
                  className="form-input"
                  placeholder="e.g. 50000 (if applicable)"
                  value={newRule.amountThreshold}
                  onChange={(e) => setNewRule({ ...newRule, amountThreshold: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Transaction Count Threshold</label>
                <input
                  type="number"
                  className="form-input"
                  placeholder="e.g. 3 (if applicable)"
                  value={newRule.transactionCountThreshold}
                  onChange={(e) => setNewRule({ ...newRule, transactionCountThreshold: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Time Window (Seconds)</label>
                <input
                  type="number"
                  className="form-input"
                  placeholder="e.g. 60 (if applicable)"
                  value={newRule.windowSeconds}
                  onChange={(e) => setNewRule({ ...newRule, windowSeconds: e.target.value })}
                />
              </div>
              <div className="form-group">
                <button type="submit" className="btn btn-primary">
                  + Add Rule
                </button>
              </div>
            </form>
          </div>

          <div className="section-header">
            <h2 className="section-title">Active Fraud Detection Rules</h2>
            <button className="btn btn-secondary btn-sm" onClick={loadRules}>🔄 Refresh</button>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Code</th>
                  <th>Amount Threshold</th>
                  <th>Velocity Threshold</th>
                  <th>Window</th>
                  <th>Enabled</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((r) => (
                  <tr key={r.id}>
                    <td>#{r.id}</td>
                    <td style={{ fontWeight: '600' }}>{r.name}</td>
                    <td><code>{r.code}</code></td>
                    <td>{r.amountThreshold ? `₹${Number(r.amountThreshold).toLocaleString()}` : '-'}</td>
                    <td>{r.transactionCountThreshold ? `${r.transactionCountThreshold} transfers` : '-'}</td>
                    <td>{r.windowSeconds ? `${r.windowSeconds}s` : '-'}</td>
                    <td>
                      <span className={`badge ${r.enabled ? 'badge-completed' : 'badge-blocked'}`}>
                        {r.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className={`btn btn-sm ${r.enabled ? 'btn-danger' : 'btn-success'}`}
                          onClick={() => handleToggleRule(r.id, r.enabled)}
                        >
                          {r.enabled ? 'Disable' : 'Enable'}
                        </button>
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => setEditingRule({ ...r })}
                        >
                          ✏️ Edit
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Edit Rule Modal */}
          {editingRule && (
            <div className="modal-overlay">
              <div className="modal-content">
                <div className="modal-header">
                  <h3>Edit Rule: {editingRule.name}</h3>
                  <button className="alert-close" onClick={() => setEditingRule(null)}>×</button>
                </div>
                <form onSubmit={handleUpdateRule}>
                  <div className="form-group" style={{ marginBottom: '12px' }}>
                    <label>Rule Name</label>
                    <input
                      type="text"
                      className="form-input"
                      value={editingRule.name || ''}
                      onChange={(e) => setEditingRule({ ...editingRule, name: e.target.value })}
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: '12px' }}>
                    <label>Description</label>
                    <input
                      type="text"
                      className="form-input"
                      value={editingRule.description || ''}
                      onChange={(e) => setEditingRule({ ...editingRule, description: e.target.value })}
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: '12px' }}>
                    <label>Amount Threshold (INR)</label>
                    <input
                      type="number"
                      step="0.01"
                      className="form-input"
                      value={editingRule.amountThreshold || ''}
                      onChange={(e) => setEditingRule({ ...editingRule, amountThreshold: e.target.value })}
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: '12px' }}>
                    <label>Transaction Count Threshold</label>
                    <input
                      type="number"
                      className="form-input"
                      value={editingRule.transactionCountThreshold || ''}
                      onChange={(e) => setEditingRule({ ...editingRule, transactionCountThreshold: e.target.value })}
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: '12px' }}>
                    <label>Window Seconds</label>
                    <input
                      type="number"
                      className="form-input"
                      value={editingRule.windowSeconds || ''}
                      onChange={(e) => setEditingRule({ ...editingRule, windowSeconds: e.target.value })}
                    />
                  </div>
                  <div className="modal-actions">
                    <button type="button" className="btn btn-secondary" onClick={() => setEditingRule(null)}>
                      Cancel
                    </button>
                    <button type="submit" className="btn btn-primary">
                      Save Changes
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
