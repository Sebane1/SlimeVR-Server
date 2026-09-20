import { SVG } from 'react-syntax-highlighter';

export function PluginBoneIcon({ name, tabName }: { name?: string; tabName?: string }) {
  const iconPath = `/icons/${name || 'plugin-default'}.svg`;
  
  // Default plugin icon if no specific icon is provided
  const defaultIcon = (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <path d="M12 2L15.6 8.4L22 9.6L17 14.4L18 21L12 17.4L6 21L7 14.4L2 9.6L8.4 8.4L12 2Z" stroke="currentColor" strokeWidth="2" fill="none"/>
    </svg>
  );

  return (
    <div className="flex items-center justify-center">
      {name ? (
        // Load custom plugin icon if provided
        <img 
          src={iconPath} 
          alt={tabName || name}
          className="w-5 h-5"
          onError={(e) => {
            // Fallback to default icon if image fails to load
            e.currentTarget.style.display = 'none';
          }}
        />
      ) : (
        // Use default plugin icon
        defaultIcon
      )}
    </div>
  );
}
