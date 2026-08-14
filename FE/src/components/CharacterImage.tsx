type CharacterImageProps = {
  alt: string
  className?: string
  src: string
}

export function CharacterImage({ alt, className, src }: CharacterImageProps) {
  return <img className={className} src={src} alt={alt} />
}
