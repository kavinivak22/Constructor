import imageCompression from 'browser-image-compression';

/**
 * Compresses an image file with high-quality settings.
 * Ensures good visual fidelity (QHD resolution) while optimizing file size.
 * @param file - The image file to compress
 * @returns Promise<File> - The compressed image file
 */
export async function compressImage(file: File): Promise<File> {
    const options = {
        maxSizeMB: 1.5, // 1.5MB max size for high quality
        maxWidthOrHeight: 2560, // QHD resolution (2560px)
        useWebWorker: true,
        initialQuality: 0.8, // Good balance of quality and size
        fileType: 'image/jpeg', // Standardize on JPEG for consistent compression
    };

    try {
        const compressedFile = await imageCompression(file, options);
        return compressedFile;
    } catch (error) {
        console.error('Image compression failed:', error);
        // Fallback to original file if compression fails
        return file;
    }
}
