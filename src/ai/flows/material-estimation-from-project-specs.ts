'use server';
/**
 * @fileOverview Generates material estimations based on project specifications.
 *
 * - estimateMaterials - A function that generates material estimations.
 * - MaterialEstimationInput - The input type for the estimateMaterials function.
 * - MaterialEstimationOutput - The return type for the estimateMaterials function.
 */

import {ai} from '@/ai/genkit';
import {z} from 'genkit';
import { StreamableValue, createStreamableValue } from 'ai/rsc';

const MaterialEstimationInputSchema = z.object({
  projectType: z.string().describe('The type of the project (e.g., residential, commercial).'),
  projectSize: z.string().describe('The size of the project in square feet.'),
  projectLocation: z.string().describe('The location of the project.'),
  specificRequirements: z.string().describe('Any specific requirements for the project.'),
});
export type MaterialEstimationInput = z.infer<typeof MaterialEstimationInputSchema>;

const MaterialEstimationOutputSchema = z.object({
  estimation: z.string().describe('The estimated materials needed for the project.'),
});
export type MaterialEstimationOutput = z.infer<typeof MaterialEstimationOutputSchema>;

export async function estimateMaterials(input: MaterialEstimationInput): Promise<MaterialEstimationOutput> {
  return estimateMaterialsFlow(input);
}

export async function getMaterialEstimationStream(
  input: MaterialEstimationInput,
  stream: StreamableValue<string>
) {
  const { stream: responseStream } = await estimateMaterialsStreamFlow(input);

  for await (const chunk of responseStream) {
    stream.update(chunk.text || '');
  }

  stream.done();
}

const prompt = ai.definePrompt({
  name: 'materialEstimationPrompt',
  input: {schema: MaterialEstimationInputSchema},
  output: {schema: MaterialEstimationOutputSchema},
  prompt: `You are an expert construction project manager. Based on the project specifications provided, estimate the materials needed for the project.

Project Type: {{{projectType}}}
Project Size: {{{projectSize}}} square feet
Project Location: {{{projectLocation}}}
Specific Requirements: {{{specificRequirements}}}

Provide a detailed estimation of the necessary materials.
`,
});

const estimateMaterialsFlow = ai.defineFlow(
  {
    name: 'estimateMaterialsFlow',
    inputSchema: MaterialEstimationInputSchema,
    outputSchema: MaterialEstimationOutputSchema,
  },
  async input => {
    const {output} = await prompt(input);
    return output!;
  }
);

const estimateMaterialsStreamFlow = ai.defineFlow(
  {
    name: 'estimateMaterialsStreamFlow',
    inputSchema: MaterialEstimationInputSchema,
  },
  async input => {
    return await ai.generate({
      prompt: `You are an expert construction project manager. Based on the project specifications provided, estimate the materials needed for the project.

      Project Type: ${input.projectType}
      Project Size: ${input.projectSize} square feet
      Project Location: ${input.projectLocation}
      Specific Requirements: ${input.specificRequirements}
      
      Provide a detailed estimation of the necessary materials.`,
      stream: true,
    });
  }
);
