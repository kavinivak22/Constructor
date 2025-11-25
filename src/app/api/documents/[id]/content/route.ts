import { createRouteHandlerClient } from '@supabase/auth-helpers-nextjs';
import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

export async function GET(
    request: Request,
    { params }: { params: { id: string } }
) {
    const supabase = createRouteHandlerClient({ cookies });

    try {
        const { id } = params;

        // 1. Fetch document metadata to get the URL
        const { data: doc, error: docError } = await supabase
            .from('documents')
            .select('*')
            .eq('id', id)
            .single();

        if (docError || !doc) {
            return new NextResponse('Document not found', { status: 404 });
        }

        // 2. Fetch the actual file content from the storage URL
        // Since we are using public URLs in this app, we can just fetch it.
        // If using private buckets, we would use supabase.storage.download()
        const response = await fetch(doc.url);

        if (!response.ok) {
            return new NextResponse('Failed to fetch document content', { status: response.status });
        }

        const blob = await response.blob();
        const headers = new Headers();
        headers.set('Content-Type', doc.type || 'application/octet-stream');
        headers.set('Content-Length', doc.size.toString());
        // Optional: Content-Disposition to force download or inline
        // headers.set('Content-Disposition', `inline; filename="${doc.name}"`);

        return new NextResponse(blob, {
            status: 200,
            headers,
        });

    } catch (error) {
        console.error('Proxy error:', error);
        return new NextResponse('Internal Server Error', { status: 500 });
    }
}
