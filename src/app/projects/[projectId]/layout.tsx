export function generateStaticParams() {
  return [{ projectId: 'default' }];
}

export default function ProjectLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
