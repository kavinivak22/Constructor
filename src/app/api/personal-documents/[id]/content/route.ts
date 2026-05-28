import { createClient } from '@/utils/supabase/server';
import { NextResponse } from 'next/server';

export async function GET(
    request: Request,
    { params }: { params: Promise<{ id: string }> }
) {
    const supabase = await createClient();

    try {
        const { id } = await params;

        // 1. Get authenticated user
        const { data: { user }, error: userError } = await supabase.auth.getUser();
        if (userError || !user) {
            return new NextResponse('Unauthorized', { status: 401 });
        }

        // 2. Fetch document metadata
        const { data: doc, error: docError } = await supabase
            .from('personal_documents')
            .select('*')
            .eq('id', id)
            .single();

        if (docError || !doc) {
            return new NextResponse('Document not found', { status: 404 });
        }

        // 3. Verify ownership
        if (doc.user_id !== user.id) {
            return new NextResponse('Forbidden', { status: 403 });
        }

        // 4. Extract storage path from the URL
        const urlParts = doc.url.split('/personal-documents/');
        const storagePath = urlParts[1];

        if (!storagePath) {
            return new NextResponse('Invalid document path', { status: 400 });
        }

        // 5. Download the file from private storage bucket
        const { data: fileData, error: downloadError } = await supabase.storage
            .from('personal-documents')
            .download(decodeURIComponent(storagePath));

        if (downloadError || !fileData) {
            console.error('Download error:', downloadError);
            return new NextResponse('Failed to retrieve file content', { status: 500 });
        }

        const headers = new Headers();
        headers.set('Content-Type', doc.type || 'application/octet-stream');
        headers.set('Content-Length', doc.size.toString());
        headers.set('Content-Disposition', `inline; filename="${encodeURIComponent(doc.name)}"`);

        return new NextResponse(fileData, {
            status: 200,
            headers,
        });

    } catch (error) {
        console.error('Personal document proxy error:', error);
        return new NextResponse('Internal Server Error', { status: 500 });
    }
}
