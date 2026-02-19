// LoanFlow MongoDB Initialization

db = db.getSiblingDB('loanflow_docs');

// Documents collection
db.createCollection('documents');
db.documents.createIndex({ loanId: 1 });
db.documents.createIndex({ customerId: 1 });
db.documents.createIndex({ documentType: 1 });
db.documents.createIndex({ status: 1 });

// Policies collection
db.createCollection('policies');
db.policies.createIndex({ policyId: 1 }, { unique: true });
db.policies.createIndex({ code: 1 }, { unique: true });
db.policies.createIndex({ state: 1, priority: 1 });
db.policies.createIndex({ category: 1 });
db.policies.createIndex({ loanProducts: 1 });

// Decision logs collection
db.createCollection('decision_logs');
db.decision_logs.createIndex({ applicationId: 1 });
db.decision_logs.createIndex({ evaluatedAt: -1 });

// Audit logs (capped collection)
db.createCollection('audit_logs', { capped: true, size: 1073741824, max: 1000000 });
db.audit_logs.createIndex({ entityId: 1 });
db.audit_logs.createIndex({ performedAt: -1 });

print('MongoDB initialization completed');
