const prescriptionDatabase = db.getSiblingDB(process.env.MONGO_APP_DATABASE || 'prescriptions');
if (!prescriptionDatabase.getUser(process.env.MONGO_APP_USERNAME)) {
  prescriptionDatabase.createUser({
    user: process.env.MONGO_APP_USERNAME,
    pwd: process.env.MONGO_APP_PASSWORD,
    roles: [{ role: 'readWrite', db: prescriptionDatabase.getName() }]
  });
}
