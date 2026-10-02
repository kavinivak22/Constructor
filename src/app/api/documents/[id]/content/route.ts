import { createClient } from '@/utils/supabase/server';
import { NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

export async function GET(
    request: Request,
    { params }: { params: Promise<{ id: string }> }
) {
    const supabase = await createClient();

    try {
        const { id } = await params;

        // 1. Fetch document metadata to get the URL
        const { data: doc, error: docError } = await supabase
            .from('documents')
            .select('*')
            .eq('id', id)
            .single();

        if (docError || !doc) {
            console.error('Document not found in DB:', docError);
            return new NextResponse('Document not found', { status: 404 });
        }

        // 2. Try fetching from public URL first
        let fileBlob: Blob | null = null;
        const contentType = doc.type || 'application/octet-stream';

        try {
            const response = await fetch(doc.url);
            if (response.ok) {
                fileBlob = await response.blob();
            }
        } catch (fetchErr) {
            console.warn('Direct fetch from doc.url failed, attempting storage download:', fetchErr);
        }

        // 3. Fallback: if fetch failed, download directly from storage bucket
        if (!fileBlob) {
            const urlParts = doc.url.split('/project-documents/');
            const storagePath = urlParts[1];
            if (storagePath) {
                const { data: storageData, error: downloadError } = await supabase.storage
                    .from('project-documents')
                    .download(decodeURIComponent(storagePath));

                if (storageData && !downloadError) {
                    fileBlob = storageData;
                } else {
                    console.error('Storage download failed:', downloadError);
                }
            }
        }

        if (!fileBlob) {
            return new NextResponse('Failed to fetch document content', { status: 404 });
        }

        const headers = new Headers();
        headers.set('Content-Type', contentType);
        headers.set('Content-Length', fileBlob.size.toString());
        headers.set('Cache-Control', 'public, max-age=3600');

        return new NextResponse(fileBlob, {
            status: 200,
            headers,
        });

    } catch (error) {
        console.error('Proxy error:', error);
        return new NextResponse('Internal Server Error', { status: 500 });
    }
}
