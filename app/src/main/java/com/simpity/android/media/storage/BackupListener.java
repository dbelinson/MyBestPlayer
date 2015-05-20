package com.simpity.android.media.storage;

public interface BackupListener {
	void StorageBackupStarted();
	void StorageBackupFinished();
	void StorageRestoreStarted();
	void StorageRestoreFinished();
	void StorageBackupError(String error);
}