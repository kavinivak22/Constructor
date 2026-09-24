/**
 * Helper utilities for worklogs: smart role matching, contractor profile lookup, and costing calculations.
 */

// Normalized comparison for worker roles (handles "MC" vs "MC (Mason Coolie)", "FC" vs "FC (Female Coolie)", casing, spaces)
export function matchesWorkerType(roleA: string, roleB: string): boolean {
    if (!roleA || !roleB) return false;
    const a = roleA.toLowerCase().trim();
    const b = roleB.toLowerCase().trim();
    if (a === b) return true;

    // Strip parenthetical text, e.g. "mc (mason coolie)" -> "mc"
    const aClean = a.replace(/\s*\(.*?\)\s*/g, '').trim();
    const bClean = b.replace(/\s*\(.*?\)\s*/g, '').trim();
    if (aClean && bClean && aClean === bClean) return true;

    // Extract inside parentheses, e.g. "mc (mason coolie)" -> "mason coolie"
    const aInside = (a.match(/\((.*?)\)/)?.[1] || '').trim();
    const bInside = (b.match(/\((.*?)\)/)?.[1] || '').trim();
    if (aInside && (aInside === b || aInside === bClean)) return true;
    if (bInside && (bInside === a || bInside === aClean)) return true;

    // Standard construction role abbreviations and aliases
    const aliasesGroup: Record<string, string[]> = {
        mc: ['mc', 'mason coolie', 'male coolie'],
        fc: ['fc', 'female coolie'],
        mason: ['mason', 'head mason', 'masonry'],
        barbender: ['bar bender', 'barbender', 'steel fixer', 'rebar worker'],
        carpenter: ['carpenter', 'shuttering carpenter'],
        electrician: ['electrician'],
        plumber: ['plumber'],
        helper: ['helper', 'coolie', 'laborer', 'labourer'],
        supervisor: ['supervisor', 'site supervisor', 'foreman']
    };

    for (const aliases of Object.values(aliasesGroup)) {
        const aMatches = aliases.some(alias => a === alias || aClean === alias);
        const bMatches = aliases.some(alias => b === alias || bClean === alias);
        if (aMatches && bMatches) return true;
    }

    return false;
}

// Case-insensitive & substring match for contractor names
export function matchesContractor(nameA: string, nameB: string): boolean {
    if (!nameA || !nameB) return false;
    const a = nameA.toLowerCase().trim();
    const b = nameB.toLowerCase().trim();
    return a === b || a.includes(b) || b.includes(a);
}

// Find matching salary profile for a contractor name
export function findContractorProfile(contractorName: string, profiles: any[]): any {
    if (!contractorName || !profiles?.length) return null;
    return profiles.find((p: any) => {
        const pName = (p.contractors?.name || p.worker_name || '').toLowerCase().trim();
        if (!pName) return false;
        return matchesContractor(contractorName, pName);
    });
}

// Find material unit cost from project materials list
export function getMaterialUnitCost(m: any, materialsList: any[]): number {
    const mName = (m.material_name || m.materialName || '').toLowerCase().trim();
    const mId = m.project_material_id || m.projectMaterialId;
    const match = materialsList.find(pm => (mId && pm.id === mId) || (pm.name && pm.name.toLowerCase().trim() === mName));
    return match ? Number(match.cost || 0) : 0;
}

// Calculate labor cost for a single labor entry
export function calculateLaborEntryCost(entry: any, salaryProfiles: any[]) {
    const contractorName = entry.contractor_name || entry.contractorName || '';
    const profile = findContractorProfile(contractorName, salaryProfiles);
    
    let entryTotal = 0;
    const workersBreakdown = (entry.workers || []).map((w: any) => {
        const count = Number(w.count || 0);
        const wType = (w.worker_type || w.workerType || '').trim();

        let rate = 0;
        let hasProfileRate = false;

        // 1. Check contractor-specific rates dictionary
        if (profile) {
            const rates = (profile.rates as Record<string, number>) || {};
            const matchingKey = Object.keys(rates).find(k => matchesWorkerType(k, wType));
            if (matchingKey && Number(rates[matchingKey]) > 0) {
                rate = Number(rates[matchingKey]);
                hasProfileRate = true;
            } else if (Number(profile.rate) > 0) {
                rate = Number(profile.rate);
                hasProfileRate = true;
            }
        }

        // 2. Fallback to any profile matching the worker type
        if (!hasProfileRate && salaryProfiles?.length > 0) {
            for (const p of salaryProfiles) {
                const rates = (p.rates as Record<string, number>) || {};
                const matchingKey = Object.keys(rates).find(k => matchesWorkerType(k, wType));
                if (matchingKey && Number(rates[matchingKey]) > 0) {
                    rate = Number(rates[matchingKey]);
                    hasProfileRate = true;
                    break;
                }
            }
        }

        const subtotal = count * rate;
        entryTotal += subtotal;

        return {
            type: wType,
            count,
            rate,
            subtotal,
            hasProfileRate,
        };
    });

    return {
        contractorName,
        profileFound: !!profile,
        entryTotal,
        workersBreakdown,
    };
}
