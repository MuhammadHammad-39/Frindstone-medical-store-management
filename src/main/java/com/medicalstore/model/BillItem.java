package com.medicalstore.model;

public class BillItem {
    private long medicineId;
    private String medicineName;
    private int quantity;
    private double unitPrice;
    private String prescriptionReference;

    public BillItem(long medicineId, String medicineName, int quantity, double unitPrice, String prescriptionReference) {
        setMedicineId(medicineId); setMedicineName(medicineName); setQuantity(quantity); setUnitPrice(unitPrice); setPrescriptionReference(prescriptionReference);
    }
    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long id) { if (id <= 0) throw new IllegalArgumentException("Medicine ID must be positive"); this.medicineId = id; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String name) { if (name == null || name.isBlank()) throw new IllegalArgumentException("Medicine name is required"); this.medicineName = name.trim(); }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive"); this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double price) { if (!Double.isFinite(price) || price < 0) throw new IllegalArgumentException("Price must be zero or more"); this.unitPrice = price; }
    public String getPrescriptionReference() { return prescriptionReference; }
    public void setPrescriptionReference(String reference) { this.prescriptionReference = reference == null ? "" : reference.trim(); }
    public double getLineTotal() { return quantity * unitPrice; }
}