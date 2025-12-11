import pandas as pd
import json
from datetime import datetime

def generar_json_instituciones():
    """
    Lee el archivo instituciones.xlsx y genera un JSON con las instituciones activas.
    """
    
    print("=" * 70)
    print("🏛️  GENERADOR DE JSON - INSTITUCIONES EDUCATIVAS")
    print("=" * 70)
    
    # Ruta del archivo
    archivo_entrada = "src/main/resources/data/instituciones.xlsx"
    archivo_salida = "src/main/resources/data/instituciones_validas.json"
    
    try:
        # Leer el archivo Excel
        print(f"\n📂 Leyendo archivo: {archivo_entrada}")
        df = pd.read_excel(archivo_entrada)
        
        print(f"   ✅ Archivo cargado: {len(df)} registros totales")
        
        # Filtrar solo instituciones ACTIVAS
        if 'ESTADO' in df.columns:
            df_activas = df[df['ESTADO'].str.upper() == 'ACTIVA'].copy()
            print(f"   ✅ Instituciones activas: {len(df_activas)}")
        else:
            df_activas = df.copy()
            print(f"   ⚠️  No se encontró columna ESTADO, usando todas")
        
        # Extraer solo el nombre de la institución
        if 'NOMBRE_INSTITUCIÓN' not in df.columns:
            print(f"   ❌ ERROR: No se encontró la columna 'NOMBRE_INSTITUCIÓN'")
            print(f"   📋 Columnas disponibles: {list(df.columns)}")
            return
        
        # Crear lista de instituciones
        instituciones = df_activas['NOMBRE_INSTITUCIÓN'].dropna().unique().tolist()
        
        # Limpiar nombres (quitar espacios extras, etc.)
        instituciones = [nombre.strip() for nombre in instituciones if nombre.strip()]
        instituciones.sort()  # Ordenar alfabéticamente
        
        # Crear estructura JSON
        datos_json = {
            "metadata": {
                "fecha_generacion": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "total_instituciones": len(instituciones),
                "fuente": "SNIES - Sistema Nacional de Información de Educación Superior",
                "filtro": "Solo instituciones con estado ACTIVA"
            },
            "instituciones": instituciones
        }
        
        # Guardar JSON
        print(f"\n💾 Guardando JSON en: {archivo_salida}")
        with open(archivo_salida, 'w', encoding='utf-8') as f:
            json.dump(datos_json, f, ensure_ascii=False, indent=2)
        
        print(f"   ✅ Archivo JSON creado exitosamente")
        
        # Mostrar estadísticas
        print("\n" + "=" * 70)
        print("📊 ESTADÍSTICAS")
        print("=" * 70)
        print(f"Total de instituciones válidas: {len(instituciones)}")
        print(f"\nPrimeras 10 instituciones:")
        for i, inst in enumerate(instituciones[:10], 1):
            print(f"  {i}. {inst}")
        
        if len(instituciones) > 10:
            print(f"  ... y {len(instituciones) - 10} más")
        
        # Generar también versión simplificada (solo array)
        archivo_simple = "src/main/resources/data/instituciones_lista.json"
        with open(archivo_simple, 'w', encoding='utf-8') as f:
            json.dump(instituciones, f, ensure_ascii=False, indent=2)
        
        print(f"\n✅ También se generó versión simplificada: {archivo_simple}")
        print("=" * 70 + "\n")
        
        return instituciones
        
    except FileNotFoundError:
        print(f"\n❌ ERROR: No se encontró el archivo '{archivo_entrada}'")
        print("   Verifica que el archivo existe en la ruta correcta")
    except Exception as e:
        print(f"\n❌ ERROR: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    generar_json_instituciones()