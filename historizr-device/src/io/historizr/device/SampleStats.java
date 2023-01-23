package io.historizr.device;

public final class SampleStats {
	public volatile long receivedCount;
	public volatile long receivedBytes;
	public volatile long processedCount;
	public volatile long skippedCount;
	public volatile long publishedCount;
	public volatile long publishedBytes;

	public final SampleStats copy() {
		var dst = new SampleStats();
		dst.receivedCount = receivedCount;
		dst.receivedBytes = receivedBytes;
		dst.processedCount = processedCount;
		dst.skippedCount = skippedCount;
		dst.publishedCount = publishedCount;
		dst.publishedBytes = publishedBytes;
		return dst;
	}
}