const RESEND_API_KEY = "re_9jaUmuqb_KSsrYGTYAnZmGcME72MQiC1v";

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

Deno.serve(async (req) => {
    // Handle CORS preflight requests
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        const { email, role, companyName, inviteLink } = await req.json()

        const res = await fetch('https://api.resend.com/emails', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${RESEND_API_KEY}`,
            },
            body: JSON.stringify({
                from: 'Constructor App <onboarding@resend.dev>',
                to: [email],
                subject: `You've been invited to join ${companyName} on Constructor`,
                html: `
          <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
            <h2>You've been invited!</h2>
            <p>You have been invited to join <strong>${companyName}</strong> as a <strong>${role}</strong>.</p>
            <p>Click the button below to accept your invitation and get started:</p>
            <a href="${inviteLink}" style="display: inline-block; background-color: #000; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 16px;">Accept Invitation</a>
            <p style="margin-top: 24px; color: #666; font-size: 14px;">If you were not expecting this invitation, you can ignore this email.</p>
          </div>
        `,
            }),
        })

        let data;
        try {
            const text = await res.text();
            try {
                data = JSON.parse(text);
            } catch {
                data = { message: text };
            }
        } catch (e) {
            data = { message: 'Failed to read response from Resend' };
        }

        if (!res.ok) {
            return new Response(JSON.stringify({ success: false, error: data }), {
                headers: { ...corsHeaders, 'Content-Type': 'application/json' },
                status: 200,
            })
        }

        return new Response(JSON.stringify({ success: true, data }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 200,
        })
    } catch (error: any) {
        return new Response(JSON.stringify({ success: false, error: error.message || 'Unknown error' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 200,
        })
    }
})
