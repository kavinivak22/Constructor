import { SidebarTrigger } from "@/components/ui/sidebar";
import { type LucideIcon } from "lucide-react";

interface PlaceholderPageProps {
  title: string;
  description: string;
  icon: LucideIcon;
  noHeader?: boolean;
  children?: React.ReactNode;
}

export function PlaceholderPage({ title, description, icon: Icon, noHeader, children }: PlaceholderPageProps) {
  const content = (
    <div className="flex flex-col items-center justify-center h-full text-center rounded-lg border-2 border-dashed bg-card/50 p-6">
        <div className="flex items-center justify-center w-16 h-16 mb-4 rounded-full bg-muted">
            <Icon className="w-8 h-8 text-muted-foreground" />
        </div>
        <h2 className="text-2xl font-bold font-headline">{title}</h2>
        <p className="max-w-sm mt-2 text-muted-foreground">{description}</p>
    </div>
  );

  if (noHeader) {
    return content;
  }
  
  return (
    <div className="flex flex-col h-full">
      <header className="flex items-center gap-2 p-4 border-b md:p-6 shrink-0 bg-background sticky top-0 z-10">
          <h1 className="text-2xl font-bold tracking-tight font-headline">
            {title}
          </h1>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        {children ? children : content}
      </main>
    </div>
  );
}
