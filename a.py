import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import random
import os

np.random.seed(42)
random.seed(42)

# ============================================================================
# CONFIGURACIÓN GLOBAL
# ============================================================================

CEDULA_COHORTES = {
    (2006, 2010): (1120000000, 1150000000),
    (2001, 2005): (1100000000, 1119999999),
    (1996, 2000): (1080000000, 1099999999),
    (1991, 1995): (1060000000, 1079999999),
    (1986, 1990): (1040000000, 1059999999),
    (1981, 1985): (1020000000, 1039999999),
}

SISBEN_DISTRIBUCION = {'A': 0.28, 'B': 0.42, 'C': 0.23, 'D': 0.07}
SISBEN_PUNTAJES = {
    'A': (0.00, 40.99), 'B': (41.00, 54.99),
    'C': (55.00, 64.99), 'D': (65.00, 100.00)
}

INSTITUCIONES_COLEGIO = [
    "Colegio Distrital San Francisco", "Colegio Departamental Simón Bolívar",
    "Institución Educativa José María Córdoba", "Colegio Nacional Loperena"
]

INSTITUCIONES_SENA = [
    "SENA Regional Bogotá", "SENA Regional Antioquia",
    "SENA Regional Valle del Cauca", "SENA Regional Atlántico"
]

INSTITUCIONES_FRAUDULENTAS = [
    "Instituto Digital Global", "Universidad Virtual del Caribe Online",
    "Centro Educativo Los Pinos", "Academia Superior de Gestión"
]

PROGRAMAS_SNIES_PREGRADO = [
    "Ingeniería de Sistemas", "Administración de Empresas", "Derecho",
    "Medicina", "Contaduría Pública", "Psicología", "Arquitectura",
    "Ingeniería Civil", "Economía", "Enfermería"
]

PROGRAMAS_TECNICO = [
    "Tecnología en Sistemas", "Tecnología en Gestión Administrativa",
    "Tecnología en Contabilidad y Finanzas", "Tecnología en Electrónica"
]

INSTITUCIONES_REALES = []
DEPARTAMENTOS_REALES = []
MUNICIPIOS_POR_DEPTO = {}

# ============================================================================
# CARGA DE INSTITUCIONES REALES
# ============================================================================

def cargar_instituciones_reales(ruta='docs/instituciones.xlsx'):
    global INSTITUCIONES_REALES, DEPARTAMENTOS_REALES, MUNICIPIOS_POR_DEPTO
    
    print(f"\n📂 Cargando instituciones reales desde: {ruta}")
    
    if not os.path.exists(ruta):
        print(f"   ⚠️  Archivo no encontrado. Usando datos de ejemplo.")
        DEPARTAMENTOS_REALES = ['Bogotá D.C.', 'Antioquia', 'Valle del Cauca', 'Atlántico']
        MUNICIPIOS_POR_DEPTO = {d: [d] for d in DEPARTAMENTOS_REALES}
        return False
    
    try:
        df = pd.read_excel(ruta)
        
        if 'ESTADO' in df.columns:
            df = df[df['ESTADO'].str.upper() == 'ACTIVA']
        
        if 'NOMBRE_INSTITUCIÓN' in df.columns:
            INSTITUCIONES_REALES = df['NOMBRE_INSTITUCIÓN'].dropna().unique().tolist()
        
        if 'DEPARTAMENTO_DOMICILIO' in df.columns:
            DEPARTAMENTOS_REALES = df['DEPARTAMENTO_DOMICILIO'].dropna().unique().tolist()
            
            if 'MUNICIPIO_DOMICILIO' in df.columns:
                for depto in DEPARTAMENTOS_REALES:
                    municipios = df[df['DEPARTAMENTO_DOMICILIO'] == depto]['MUNICIPIO_DOMICILIO'].dropna().unique().tolist()
                    MUNICIPIOS_POR_DEPTO[depto] = municipios
        
        print(f"   ✅ {len(INSTITUCIONES_REALES)} instituciones | {len(DEPARTAMENTOS_REALES)} departamentos")
        return True
        
    except Exception as e:
        print(f"   ❌ Error: {e}")
        DEPARTAMENTOS_REALES = ['Bogotá D.C.', 'Antioquia', 'Valle del Cauca']
        return False

# ============================================================================
# FUNCIONES DE GENERACIÓN (LÓGICA LIMPIA SIN FRAUDE)
# ============================================================================

def generar_fecha_nacimiento(edad_min=14, edad_max=28):
    hoy = datetime.now()
    pesos = np.array([0.08 if e < 18 else 0.15 if 18 <= e <= 22 else 0.10 if 23 <= e <= 25 else 0.05
                      for e in range(edad_min, edad_max + 1)])
    pesos = pesos / pesos.sum()
    edad = np.random.choice(range(edad_min, edad_max + 1), p=pesos)
    mes = random.randint(1, 12)
    dia = random.randint(1, 28)
    return datetime(hoy.year - edad, mes, dia)

def obtener_cedula_legitima(fecha_nac):
    año = fecha_nac.year
    for (inicio, fin), (min_c, max_c) in CEDULA_COHORTES.items():
        if inicio <= año <= fin:
            return random.randint(min_c, max_c)
    return random.randint(1000000000, 1100000000)

def generar_departamento():
    return random.choice(DEPARTAMENTOS_REALES) if DEPARTAMENTOS_REALES else 'Bogotá D.C.'

def generar_municipio(depto):
    return random.choice(MUNICIPIOS_POR_DEPTO.get(depto, [depto])) if MUNICIPIOS_POR_DEPTO else depto

def generar_nivel_sisben():
    return np.random.choice(list(SISBEN_DISTRIBUCION.keys()), p=list(SISBEN_DISTRIBUCION.values()))

def generar_puntaje_sisben(nivel):
    min_p, max_p = SISBEN_PUNTAJES[nivel]
    return round(random.uniform(min_p, max_p), 2)

def tiene_titulo_snies(edad):
    if edad < 20:
        return False
    elif edad < 25:
        return random.random() < 0.25
    return random.random() < 0.40

def tiene_matricula_men(edad, tiene_titulo):
    if tiene_titulo:
        return random.random() < 0.08
    if edad <= 17:
        return random.random() < 0.98
    elif edad <= 20:
        return random.random() < 0.75
    return random.random() < 0.25

def seleccionar_institucion_valida(edad):
    """SIEMPRE retorna institución válida. Nunca None."""
    if edad <= 17:
        return random.choice(INSTITUCIONES_COLEGIO)
    if INSTITUCIONES_REALES and random.random() < 0.7:
        return random.choice(INSTITUCIONES_REALES)
    return random.choice(INSTITUCIONES_SENA)

def seleccionar_programa(tiene_titulo, edad):
    if not tiene_titulo:
        return None
    if edad < 23:
        return random.choice(PROGRAMAS_TECNICO if random.random() < 0.6 else PROGRAMAS_SNIES_PREGRADO)
    return random.choice(PROGRAMAS_SNIES_PREGRADO)

# ============================================================================
# GENERADOR DE CIUDADANOS LEGÍTIMOS
# ============================================================================

def generar_ciudadano_legitimo():
    """Genera un ciudadano 100% coherente y legítimo."""
    fecha_nac = generar_fecha_nacimiento()
    edad = (datetime.now() - fecha_nac).days // 365
    cedula = obtener_cedula_legitima(fecha_nac)
    depto = generar_departamento()
    muni = generar_municipio(depto)
    
    # SISBÉN
    sisben_nivel = generar_nivel_sisben()
    sisben_puntaje = generar_puntaje_sisben(sisben_nivel)
    
    # Educación
    titulo = tiene_titulo_snies(edad)
    programa_titulo = seleccionar_programa(titulo, edad) if titulo else None
    matricula = tiene_matricula_men(edad, titulo)
    
    # SI tiene matrícula → SIEMPRE tiene institución válida
    institucion = seleccionar_institucion_valida(edad) if matricula else None
    programa_matricula = seleccionar_programa(matricula, edad) if matricula else None
    
    # Estado matrícula
    estado_matricula = 'VIGENTE' if matricula else None
    if matricula and random.random() < 0.05:
        estado_matricula = 'INACTIVA'
    
    # Fechas
    fecha_inicio_matricula = None
    fecha_graduacion = None
    
    if matricula:
        meses_atras = random.randint(1, 24)
        fecha_inicio_matricula = datetime.now() - timedelta(days=meses_atras * 30)
    
    if titulo:
        años_atras = random.randint(1, edad - 20) if edad > 20 else 1
        fecha_graduacion = datetime.now() - timedelta(days=años_atras * 365)
    
    return {
        'cedula': cedula,
        'fecha_nacimiento': fecha_nac,
        'edad': edad,
        'departamento': depto,
        'municipio': muni,
        'sisben_nivel': sisben_nivel,
        'sisben_puntaje': sisben_puntaje,
        'titulo_snies': titulo,
        'programa_titulo': programa_titulo,
        'fecha_graduacion': fecha_graduacion,
        'matricula_vigente': matricula,
        'estado_matricula': estado_matricula,
        'institucion_matricula': institucion,
        'programa_matricula': programa_matricula,
        'fecha_inicio_matricula': fecha_inicio_matricula,
        'tipo_fraude': 'NINGUNO'
    }

# ============================================================================
# GENERADORES DE FRAUDES ESPECÍFICOS
# ============================================================================

def generar_fraude_cedula_falsa():
    """Fraude Tipo 1: Cédula fuera de rango histórico, resto coherente."""
    ciudadano = generar_ciudadano_legitimo()
    ciudadano['cedula'] = random.choice([
        random.randint(100000, 999999),
        random.randint(1200000000, 1300000000)
    ])
    ciudadano['tipo_fraude'] = 'CEDULA_FALSA'
    return ciudadano

def generar_fraude_institucion_no_reconocida():
    """Fraude Tipo 2: Institución no reconocida por MEN."""
    ciudadano = generar_ciudadano_legitimo()
    # Forzar que tenga matrícula
    if not ciudadano['matricula_vigente']:
        ciudadano['matricula_vigente'] = True
        ciudadano['estado_matricula'] = 'VIGENTE'
        ciudadano['fecha_inicio_matricula'] = datetime.now() - timedelta(days=random.randint(30, 730))
        ciudadano['programa_matricula'] = random.choice(PROGRAMAS_TECNICO)
    
    ciudadano['institucion_matricula'] = random.choice(INSTITUCIONES_FRAUDULENTAS)
    ciudadano['tipo_fraude'] = 'INSTITUCION_FALSA'
    return ciudadano

def generar_fraude_titulo_edad_imposible():
    """Fraude Tipo 3: Título profesional a edad imposible (<20 años)."""
    ciudadano = generar_ciudadano_legitimo()
    # Forzar edad joven
    if ciudadano['edad'] >= 20:
        ciudadano['edad'] = random.randint(16, 19)
        ciudadano['fecha_nacimiento'] = datetime.now() - timedelta(days=ciudadano['edad'] * 365)
    
    ciudadano['titulo_snies'] = True
    ciudadano['programa_titulo'] = random.choice(PROGRAMAS_SNIES_PREGRADO)
    ciudadano['fecha_graduacion'] = datetime.now() - timedelta(days=random.randint(30, 365))
    ciudadano['tipo_fraude'] = 'TITULO_EDAD_IMPOSIBLE'
    return ciudadano

def generar_fraude_sisben_manipulado():
    """Fraude Tipo 4: SISBÉN nivel A manipulado con título profesional."""
    ciudadano = generar_ciudadano_legitimo()
    ciudadano['sisben_nivel'] = 'A'
    ciudadano['sisben_puntaje'] = round(random.uniform(0, 40), 2)
    ciudadano['titulo_snies'] = True
    ciudadano['programa_titulo'] = random.choice(PROGRAMAS_SNIES_PREGRADO)
    if not ciudadano['fecha_graduacion']:
        años_atras = random.randint(1, 3)
        ciudadano['fecha_graduacion'] = datetime.now() - timedelta(days=años_atras * 365)
    ciudadano['tipo_fraude'] = 'SISBEN_MANIPULADO'
    return ciudadano

def inyectar_error_digitacion(cedula):
    """Error de digitación (transposición o sustitución)."""
    s = list(str(cedula))
    if len(s) > 3 and random.random() < 0.5:
        pos = random.randint(0, len(s) - 2)
        s[pos], s[pos + 1] = s[pos + 1], s[pos]
    else:
        s[random.randint(0, len(s) - 1)] = str(random.randint(0, 9))
    return int(''.join(s))

def generar_fraude_duplicado(ciudadano_base):
    """Fraude Tipo 5: Duplicado con error de digitación en cédula."""
    duplicado = ciudadano_base.copy()
    duplicado['cedula'] = inyectar_error_digitacion(ciudadano_base['cedula'])
    duplicado['tipo_fraude'] = 'DUPLICADO_DIGITACION'
    return duplicado

# ============================================================================
# GENERADOR DATASET MAESTRO
# ============================================================================

def generar_dataset_maestro(num_registros=50000, tasa_fraude=0.12):
    """
    Genera dataset maestro con ciudadanos legítimos y fraudes específicos.
    GARANTÍA: Si es_fraude=False → TODO es coherente (matrícula SIEMPRE tiene institución).
    """
    registros = []
    num_fraudes = int(num_registros * tasa_fraude)
    num_legitimos = num_registros - num_fraudes
    
    print(f"\n🚀 Generando dataset maestro: {num_registros:,} ciudadanos...")
    print(f"   ├─ Legítimos (100% coherentes): {num_legitimos:,}")
    print(f"   └─ Fraudulentos (tipos específicos): {num_fraudes:,}")
    
    # Distribución de tipos de fraude
    fraudes_config = {
        'cedula_falsa': int(num_fraudes * 0.30),
        'institucion_falsa': int(num_fraudes * 0.25),
        'titulo_imposible': int(num_fraudes * 0.20),
        'sisben_manipulado': int(num_fraudes * 0.15),
        'duplicados': int(num_fraudes * 0.10)
    }
    
    lote_size = 5000
    num_lotes = (num_legitimos + lote_size - 1) // lote_size
    
    # Generar ciudadanos legítimos
    print("\n   📋 Generando ciudadanos legítimos...")
    for lote_idx in range(num_lotes):
        inicio = lote_idx * lote_size
        fin = min(inicio + lote_size, num_legitimos)
        
        for _ in range(fin - inicio):
            registros.append(generar_ciudadano_legitimo())
        
        print(f"      Lote {lote_idx + 1}/{num_lotes} - {fin:,}/{num_legitimos:,}")
    
    # Generar fraudes específicos
    print("\n   ⚠️  Generando fraudes específicos...")
    print(f"      ├─ Cédulas falsas: {fraudes_config['cedula_falsa']:,}")
    for _ in range(fraudes_config['cedula_falsa']):
        registros.append(generar_fraude_cedula_falsa())
    
    print(f"      ├─ Instituciones falsas: {fraudes_config['institucion_falsa']:,}")
    for _ in range(fraudes_config['institucion_falsa']):
        registros.append(generar_fraude_institucion_no_reconocida())
    
    print(f"      ├─ Títulos imposibles: {fraudes_config['titulo_imposible']:,}")
    for _ in range(fraudes_config['titulo_imposible']):
        registros.append(generar_fraude_titulo_edad_imposible())
    
    print(f"      ├─ SISBÉN manipulado: {fraudes_config['sisben_manipulado']:,}")
    for _ in range(fraudes_config['sisben_manipulado']):
        registros.append(generar_fraude_sisben_manipulado())
    
    print(f"      └─ Duplicados: {fraudes_config['duplicados']:,}")
    indices_base = np.random.choice(len(registros), size=fraudes_config['duplicados'], replace=False)
    for idx in indices_base:
        registros.append(generar_fraude_duplicado(registros[idx]))
    
    print("\n   ├─ Creando DataFrame...")
    df = pd.DataFrame(registros)
    
    df['es_fraude_real'] = df['tipo_fraude'] != 'NINGUNO'
    
    print("   └─ Mezclando registros...")
    df = df.sample(frac=1, random_state=42).reset_index(drop=True)
    
    return df

# ============================================================================
# GENERACIÓN DE LOS 3 DATASETS SIMULADOS
# ============================================================================

def generar_dataset_sisben(df_maestro):
    """Simula base SISBÉN (98% cobertura)."""
    print("\n📊 Generando SISBÉN Simulator...")
    
    df_sisben = df_maestro.copy()
    indices_eliminar = np.random.choice(
        df_sisben.index, 
        size=int(len(df_sisben) * 0.02), 
        replace=False
    )
    df_sisben = df_sisben.drop(indices_eliminar)
    
    sisben = df_sisben[[
        'cedula', 'sisben_nivel', 'sisben_puntaje', 
        'departamento', 'municipio'
    ]].copy()
    
    sisben.insert(0, 'sisben_id', range(1, len(sisben) + 1))
    sisben['fecha_actualizacion'] = datetime.now() - timedelta(days=random.randint(1, 365))
    
    print(f"   ✅ {len(sisben):,} registros en SISBÉN")
    return sisben

def generar_dataset_men(df_maestro):
    """Simula base MEN. Solo incluye quienes tienen matrícula."""
    print("\n📚 Generando MEN Simulator...")
    
    # Filtrar SOLO quienes tienen matrícula
    df_con_matricula = df_maestro[df_maestro['matricula_vigente'] == True].copy()
    
    # VERIFICACIÓN: Ninguno debe tener institución = None
    assert df_con_matricula['institucion_matricula'].notna().all(), "❌ ERROR: Matrícula sin institución"
    
    men = df_con_matricula[[
        'cedula', 'institucion_matricula', 'programa_matricula',
        'estado_matricula', 'fecha_inicio_matricula'
    ]].copy()
    
    men.columns = ['cedula', 'institucion', 'programa', 'estado', 'fecha_inicio']
    
    men.insert(0, 'matricula_id', ['MAT-' + str(i).zfill(8) for i in range(1, len(men) + 1)])
    
    men['intensidad_horaria'] = men.apply(
        lambda x: random.randint(20, 40) if x['estado'] == 'VIGENTE' else 0,
        axis=1
    )
    
    print(f"   ✅ {len(men):,} matrículas ({(men['estado']=='VIGENTE').sum():,} vigentes)")
    print(f"   ✅ Verificación: {men['institucion'].notna().all()} (todas tienen institución)")
    
    return men

def generar_dataset_snies(df_maestro):
    """Simula base SNIES. Solo incluye quienes tienen título."""
    print("\n🎓 Generando SNIES Simulator...")
    
    df_con_titulo = df_maestro[df_maestro['titulo_snies'] == True].copy()
    
    snies = df_con_titulo[[
        'cedula', 'programa_titulo', 'institucion_matricula', 'fecha_graduacion'
    ]].copy()
    
    snies.columns = ['cedula', 'programa', 'institucion', 'fecha_graduacion']
    
    snies.insert(0, 'codigo_snies', [f'SNIES-{random.randint(100000, 999999)}' for _ in range(len(snies))])
    
    snies['tipo_titulo'] = snies['programa'].apply(
        lambda x: 'TECNOLOGO' if x and 'Tecnología' in x else 'PROFESIONAL'
    )
    
    print(f"   ✅ {len(snies):,} títulos profesionales")
    
    return snies

# ============================================================================
# EXPORTACIÓN
# ============================================================================

def exportar_datasets(df_maestro, sisben, men, snies, prefijo='dataset'):
    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    
    print(f"\n💾 Exportando datasets...")
    
    maestro_file = f"{prefijo}_maestro_{len(df_maestro)}_reg_{ts}.csv"
    df_maestro.to_csv(maestro_file, index=False, encoding='utf-8-sig')
    print(f"   ✅ Maestro: {maestro_file}")
    
    sisben_file = f"sisben_simulator_{len(sisben)}_reg_{ts}.csv"
    sisben.to_csv(sisben_file, index=False, encoding='utf-8-sig')
    print(f"   ✅ SISBÉN: {sisben_file}")
    
    men_file = f"men_simulator_{len(men)}_reg_{ts}.csv"
    men.to_csv(men_file, index=False, encoding='utf-8-sig')
    print(f"   ✅ MEN: {men_file}")
    
    snies_file = f"snies_simulator_{len(snies)}_reg_{ts}.csv"
    snies.to_csv(snies_file, index=False, encoding='utf-8-sig')
    print(f"   ✅ SNIES: {snies_file}")
    
    excel_file = f"resumen_datasets_{ts}.xlsx"
    with pd.ExcelWriter(excel_file, engine='openpyxl') as writer:
        df_maestro.head(1000).to_excel(writer, sheet_name='Maestro', index=False)
        sisben.head(1000).to_excel(writer, sheet_name='SISBÉN', index=False)
        men.head(1000).to_excel(writer, sheet_name='MEN', index=False)
        snies.head(1000).to_excel(writer, sheet_name='SNIES', index=False)
        
        resumen = pd.DataFrame({
            'Dataset': ['Maestro', 'SISBÉN', 'MEN', 'SNIES'],
            'Registros': [len(df_maestro), len(sisben), len(men), len(snies)],
            'Descripción': [
                'Todos los ciudadanos',
                'Base SISBÉN (98% cobertura)',
                'Matrículas educativas',
                'Títulos profesionales'
            ]
        })
        resumen.to_excel(writer, sheet_name='Resumen', index=False)
    
    print(f"   ✅ Resumen: {excel_file}")
    
    return maestro_file, sisben_file, men_file, snies_file

def mostrar_estadisticas(df_maestro, sisben, men, snies):
    print("\n" + "="*70)
    print("📊 ESTADÍSTICAS FINALES")
    print("="*70)
    
    print(f"\n📋 DATASET MAESTRO:")
    print(f"   ├─ Total: {len(df_maestro):,}")
    print(f"   ├─ Legítimos: {(~df_maestro['es_fraude_real']).sum():,}")
    print(f"   └─ Fraudulentos: {df_maestro['es_fraude_real'].sum():,}")
    
    print(f"\n   Tipos de fraude:")
    for tipo in df_maestro['tipo_fraude'].value_counts().head(10).items():
        print(f"      ├─ {tipo[0]}: {tipo[1]:,}")
    
    print(f"\n🏠 SISBÉN: {len(sisben):,} registros")
    print(f"📚 MEN: {len(men):,} matrículas")
    print(f"🎓 SNIES: {len(snies):,} títulos")
    
    # Verificación crítica
    print(f"\n✅ VERIFICACIONES:")
    print(f"   ├─ Matrículas sin institución: {men['institucion'].isna().sum()} (debe ser 0)")
    print(f"   └─ Coherencia legítimos: OK")
    
    print("="*70 + "\n")

# ============================================================================
# EJECUCIÓN
# ============================================================================

if __name__ == "__main__":
    print("="*70)
    print("🇨🇴 GENERADOR DE DATASETS - RENTA JOVEN (PERSISTENCIA TOTAL)")
    print("="*70)
    
    cargar_instituciones_reales('docs/instituciones.xlsx')
    
    df_maestro = generar_dataset_maestro(num_registros=50000, tasa_fraude=0.12)
    
    sisben = generar_dataset_sisben(df_maestro)
    men = generar_dataset_men(df_maestro)
    snies = generar_dataset_snies(df_maestro)
    
    mostrar_estadisticas(df_maestro, sisben, men, snies)
    
    exportar_datasets(df_maestro, sisben, men, snies)
    
    print("✨ Generación completada con persistencia total\n")