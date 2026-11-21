package com.medicalstore.service;

import com.medicalstore.repository.StoreRepository;

public abstract class AbstractStoreService {
    protected final StoreRepository repository;
    protected AbstractStoreService(StoreRepository repository) { this.repository = repository; }
}